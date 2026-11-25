package org.vik.torrente.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.vik.torrente.repository.TorrentRepository
import org.vik.torrente.repository.models.FileInfo
import org.vik.torrente.repository.models.PieceInfo
import org.vik.torrente.repository.models.PieceStatus as RepositoryPieceStatus
import org.vik.torrente.repository.models.TorrentId
import org.vik.torrente.repository.models.TorrentInfo
import org.vik.torrente.repository.models.TorrentModeModel
import org.vik.torrente.repository.models.TorrentStatusModel
import org.vik.torrente.repository.orm.deleteTorrent
import org.vik.torrente.repository.orm.getFilesByTorrent
import org.vik.torrente.repository.orm.getPeersByTorrent
import org.vik.torrente.repository.orm.getPiecesByTorrent
import org.vik.torrente.repository.orm.getTorrentById
import org.vik.torrente.repository.orm.getTorrents
import org.vik.torrente.repository.orm.insertTorrentFullData
import org.vik.torrente.repository.orm.updateTorrent
import org.vik.torrente.torrent.engine.TorrentMetadata
import org.vik.torrente.ui.model.ParsedTorrentInfo
import org.vik.torrente.ui.model.Peer
import org.vik.torrente.ui.model.Torrent
import org.vik.torrente.ui.model.TorrentFile
import org.vik.torrente.ui.model.Tracker
import org.vik.torrente.utils.toHex
import java.io.File
import java.util.concurrent.atomic.AtomicLong


class TorrentUIViewModel(
    private val repository: TorrentRepository,
) : ViewModel() {
    private val _torrents = MutableStateFlow<List<Torrent>>(emptyList())
    val torrents: StateFlow<List<Torrent>> = _torrents.asStateFlow()

    private var lastTimeRefreshed = AtomicLong(0)

    val downloadStates = mutableMapOf<String, Pair<Long, Long>>()

    val downloadSpeeds = mutableMapOf<String, Long>()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            var firstRefresh = true
            while (isActive) {
                try {
                    _torrents.value = withContext(Dispatchers.IO) { loadCatalog() }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    reportError(e)
                } finally {
                    if (firstRefresh) {
                        firstRefresh = false
                    }
                }
                delay(CATALOG_REFRESH_INTERVAL_MS)
            }
        }
    }

    suspend fun parseTorrent(torrentFilePath: String): ParsedTorrentInfo? = withContext(Dispatchers.IO) {
        try {
            val metadata = TorrentMetadata(File(torrentFilePath).readBytes())
            ParsedTorrentInfo(
                name = metadata.name,
                filePath = torrentFilePath,
                totalBytes = metadata.totalSize,
                files = metadata.files.mapIndexed { index, file ->
                    TorrentFile(
                        id = index.toString(),
                        path = file.path.drop(1).joinToString("/").ifEmpty { metadata.name },
                        sizeBytes = file.length,
                    )
                },
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            reportError(e)
            null
        }
    }

    fun addTorrent(
        parsedInfo: ParsedTorrentInfo,
        destinationPath: String,
        mode: TorrentModeModel,
    ): Job = viewModelScope.launch {
        try {
            require(destinationPath.isNotBlank()) { "Download destination cannot be empty" }
            require(mode.isActiveMode()) { "A torrent must be added with an active mode" }
            withContext(Dispatchers.IO) {
                val metadata = TorrentMetadata(File(parsedInfo.filePath).readBytes())
                require(metadata.name == parsedInfo.name && metadata.totalSize == parsedInfo.totalBytes) {
                    "Torrent metadata changed after it was parsed"
                }

                val hash = metadata.infoHash.toHex()
                val id = TorrentId(hash)
                check(repository.getTorrentById(id) == null) { "Torrent is already in the catalog" }

                val torrentInfo = TorrentInfo(
                    hash = hash,
                    tracker = metadata.trackers.firstOrNull().orEmpty(),
                    name = metadata.name,
                    length = metadata.totalSize,
                    status = TorrentStatusModel.CHECKING,
                    mode = mode,
                    filePath = parsedInfo.filePath,
                    downloadDir = destinationPath,
                )
                val files = metadata.files.mapIndexed { index, file ->
                    FileInfo(
                        torrent = id,
                        number = index.toLong(),
                        name = file.path.drop(1).joinToString("/").ifEmpty { metadata.name },
                        length = file.length,
                    )
                }
                val pieces = metadata.pieceHashes.mapIndexed { index, pieceHash ->
                    PieceInfo(
                        torrent = id,
                        number = index.toLong(),
                        hash = pieceHash.toHex(),
                        length = metadata.pieceSize(index).toLong(),
                        status = RepositoryPieceStatus.NOT_AVAILABLE,
                        downloadPath = destinationPath,
                    )
                }
                check(repository.insertTorrentFullData(torrentInfo, pieces, files, emptyList())) {
                    "Could not save torrent metadata. It may already exist or the database write failed."
                }
            }
            refreshCatalog()
            _error.value = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            reportError(e)
        }
    }

    fun pauseTorrent(torrentId: String): Job {
        var newMode: TorrentModeModel
        val torrent = getTorrentInfo(torrentId)
            ?: throw IllegalStateException("Torrent does not exist in the repository")
        newMode = when(torrent.mode){
            TorrentModeModel.DOWNLOADING -> TorrentModeModel.PAUSED_DOWNLOADING
            TorrentModeModel.DOWNLOADING_AND_SEEDING -> TorrentModeModel.PAUSED_DOWNLOADING_AND_SEEDING
            TorrentModeModel.SEEDING -> TorrentModeModel.PAUSED_SEEDING
            else -> torrent.mode
        }
        return updateMode(torrentId, newMode)
    }

    fun resumeTorrent(torrentId: String): Job {
        val torrent = getTorrentInfo(torrentId)
            ?: throw IllegalStateException("Torrent does not exist in the repository")
        val newMode = when(torrent.mode){
            TorrentModeModel.PAUSED_DOWNLOADING -> TorrentModeModel.DOWNLOADING
            TorrentModeModel.PAUSED_DOWNLOADING_AND_SEEDING -> TorrentModeModel.DOWNLOADING_AND_SEEDING
            TorrentModeModel.PAUSED_SEEDING -> TorrentModeModel.SEEDING
            else -> torrent.mode
        }
        return updateMode(torrentId, newMode)
    }

    fun resumeTorrent(torrentId: String, mode: TorrentModeModel): Job {
        if (!mode.isActiveMode()) {
            return viewModelScope.launch {
                reportError(IllegalArgumentException("Choose an active mode to restart the torrent"))
            }
        }

        val torrent = getTorrentInfo(torrentId)
        if (torrent?.mode != TorrentModeModel.INACTIVE) {
            return viewModelScope.launch {
                reportError(IllegalStateException("Only an inactive torrent can be restarted with a new mode"))
            }
        }

        return updateMode(torrentId, mode)
    }

    fun stopTorrent(torrentId: String): Job {
        return updateMode(torrentId, TorrentModeModel.INACTIVE)
    }

    fun deleteTorrent(torrentId: String): Job = viewModelScope.launch {
        try {
            withContext(Dispatchers.IO) {
                val id = TorrentId(torrentId)
                val current = repository.getTorrentById(id)
                    ?: throw IllegalStateException("Torrent does not exist in the repository")
                if (current.mode != TorrentModeModel.INACTIVE) {
                    check(repository.updateTorrent(current.copy(mode = TorrentModeModel.INACTIVE))) {
                        "Could not mark torrent inactive before deleting it"
                    }
                }
                check(repository.deleteTorrent(id)) { "Could not delete torrent from the repository" }
            }
            refreshCatalog()
            _error.value = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            reportError(e)
        }
    }

    fun getTorrentInfo(torrentId: String): Torrent? = _torrents.value.firstOrNull { it.id == torrentId }

    fun clearError() {
        _error.value = null
    }

    private fun updateMode(
        torrentId: String,
        mode: TorrentModeModel,
    ): Job = viewModelScope.launch {
        try {
            withContext(Dispatchers.IO) {
                val id = TorrentId(torrentId)
                val current = repository.getTorrentById(id)
                    ?: throw IllegalStateException("Torrent does not exist in the repository")
                if (current.mode != mode) {
                    check(repository.updateTorrent(current.copy(mode = mode))) {
                        "Could not update torrent mode"
                    }
                }
            }
            refreshCatalog()
            _error.value = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            reportError(e)
        }
    }

    private suspend fun refreshCatalog() {
        _torrents.value = withContext(Dispatchers.IO) { loadCatalog() }
    }

    private fun loadCatalog(): List<Torrent> = repository.getTorrents().map { info ->
        val torrentId = info.getId()
        val files = repository.getFilesByTorrent(torrentId).map { file ->
            TorrentFile(file.number.toString(), file.name, file.length)
        }
        val peers = repository.getPeersByTorrent(torrentId).map { peer ->
            Peer(peer.address, peer.status)
        }
        val pieces = repository.getPiecesByTorrent(torrentId)
        val downloaded = pieces
            .filter { it.status == RepositoryPieceStatus.AVAILABLE }
            .sumOf { it.length }
            .coerceAtMost(info.length)
        val downloadSpeed = when(val downloadState = this@TorrentUIViewModel.downloadStates[torrentId.hash]) {
            null -> {
                this@TorrentUIViewModel.downloadStates[torrentId.hash] = Pair(System.currentTimeMillis(), downloaded)
                this@TorrentUIViewModel.downloadSpeeds[torrentId.hash]?:0
            }
            else -> {
                val (lastModificationTime, lastDownloadedBytes) = downloadState
                val timedelta = System.currentTimeMillis() - lastModificationTime
                // only refresh every second
                if(timedelta > 1000){
                    val bytesDelta = downloaded - lastDownloadedBytes
                    this@TorrentUIViewModel.downloadStates[torrentId.hash] = Pair(System.currentTimeMillis(), downloaded)
                    this@TorrentUIViewModel.downloadSpeeds[torrentId.hash] =  1000 * (bytesDelta / timedelta)
                    1000 * (bytesDelta / timedelta)
                }else {
                    this@TorrentUIViewModel.downloadSpeeds[torrentId.hash]?:0
                }

            }
        }
        Torrent(
            id = info.hash,
            name = info.name,
            totalBytes = info.length,
            downloadedBytes = downloaded,
            lastModificationTime = System.currentTimeMillis(),
            downloadSpeedBytesPerSec = downloadSpeed,
            uploadSpeedBytesPerSec = 0,
            status = info.status,
            destinationPath = info.downloadDir,
            mode = info.mode,
            pieceCount = pieces.size,
            peerList = peers,
            peers = peers.size,
            trackers = info.tracker.takeIf(String::isNotBlank)?.let { listOf(Tracker(it)) }.orEmpty(),
            files = files,
        )
    }

    private fun TorrentModeModel.isActiveMode(): Boolean =
        this == TorrentModeModel.DOWNLOADING ||
            this == TorrentModeModel.DOWNLOADING_AND_SEEDING ||
            this == TorrentModeModel.SEEDING

    private fun reportError(error: Throwable) {
        _error.value = error.message ?: error::class.simpleName ?: "Torrent operation failed"
    }

    private companion object {
        const val CATALOG_REFRESH_INTERVAL_MS = 500L
    }
}
