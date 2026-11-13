package org.vik.torrente.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.vik.torrente.model.ParsedTorrentInfo
import org.vik.torrente.model.Torrent
import org.vik.torrente.model.TorrentFile
import org.vik.torrente.model.TorrentStatus
import kotlin.random.Random

/**
 * Placeholder implementation of [TorrentService] for UI prototyping and development.
 * Simulates real-time download progress, speed fluctuations, and lifecycle transitions.
 */
class MockTorrentService(
    defaultDownloadPath: String = ""
) : TorrentService {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _torrents = MutableStateFlow<List<Torrent>>(
        listOf(
            Torrent(
                id = "mock-1",
                name = "Ubuntu 24.04 LTS Desktop (64-bit)",
                totalBytes = 6_104_743_936L, // ~5.68 GB
                downloadedBytes = 4_029_130_997L, // ~66%
                downloadSpeedBytesPerSec = 4_250_000L, // ~4.25 MB/s
                uploadSpeedBytesPerSec = 145_000L,
                status = TorrentStatus.DOWNLOADING,
                destinationPath = defaultDownloadPath,
                files = listOf(
                    TorrentFile("f1", "ubuntu-24.04-desktop-amd64.iso", 6_104_743_936L, true)
                )
            ),
            Torrent(
                id = "mock-2",
                name = "Big Buck Bunny (1080p, H.264, Surround)",
                totalBytes = 885_000_000L, // ~885 MB
                downloadedBytes = 885_000_000L, // 100% completed
                downloadSpeedBytesPerSec = 0L,
                uploadSpeedBytesPerSec = 520_000L,
                status = TorrentStatus.COMPLETED,
                destinationPath = defaultDownloadPath,
                files = listOf(
                    TorrentFile("f2_1", "big_buck_bunny_1080p_surround.avi", 850_000_000L, true),
                    TorrentFile("f2_2", "poster.jpg", 25_000_000L, true),
                    TorrentFile("f2_3", "readme.txt", 10_000_000L, true)
                )
            ),
            Torrent(
                id = "mock-3",
                name = "Debian 12.5 DVD NetInst (AMD64)",
                totalBytes = 734_000_000L,
                downloadedBytes = 198_000_000L, // ~27%
                downloadSpeedBytesPerSec = 0L,
                uploadSpeedBytesPerSec = 0L,
                status = TorrentStatus.PAUSED,
                destinationPath = defaultDownloadPath,
                files = listOf(
                    TorrentFile("f3_1", "debian-12.5.0-amd64-netinst.iso", 734_000_000L, true)
                )
            )
        )
    )

    override val torrents: StateFlow<List<Torrent>> = _torrents.asStateFlow()

    init {
        startSimulationLoop()
    }

    private fun startSimulationLoop() {
        scope.launch {
            while (isActive) {
                delay(1000)
                _torrents.value = _torrents.value.map { torrent ->
                    if (torrent.status == TorrentStatus.DOWNLOADING) {
                        // Fluctuate speed slightly between 2.0 MB/s and 5.5 MB/s
                        val jitteredSpeed = (2_000_000L..5_500_000L).random()
                        val newDownloaded = (torrent.downloadedBytes + jitteredSpeed).coerceAtMost(torrent.totalBytes)
                        val isNowFinished = newDownloaded >= torrent.totalBytes

                        torrent.copy(
                            downloadedBytes = newDownloaded,
                            downloadSpeedBytesPerSec = if (isNowFinished) 0L else jitteredSpeed,
                            status = if (isNowFinished) TorrentStatus.COMPLETED else TorrentStatus.DOWNLOADING
                        )
                    } else {
                        torrent
                    }
                }
            }
        }
    }

    override suspend fun parseTorrent(filePath: String, fileName: String): ParsedTorrentInfo {
        // Mock parsing logic: extract meaningful display name and generate representative files
        val cleanName = fileName.removeSuffix(".torrent").replace("_", " ").replace(".", " ")

        val sampleFiles = when {
            fileName.contains("bunny", ignoreCase = true) -> listOf(
                TorrentFile("f_1", "$cleanName.mkv", 850_000_000L, true),
                TorrentFile("f_2", "trailer.mp4", 45_000_000L, true),
                TorrentFile("f_3", "cover.jpg", 2_500_000L, true),
                TorrentFile("f_4", "subtitles_en.srt", 85_000L, true)
            )
            fileName.contains("wired", ignoreCase = true) -> listOf(
                TorrentFile("w_1", "track01_intro.flac", 42_000_000L, true),
                TorrentFile("w_2", "track02_main.flac", 68_000_000L, true),
                TorrentFile("w_3", "track03_outro.flac", 55_000_000L, true),
                TorrentFile("w_4", "artwork.png", 5_000_000L, true)
            )
            else -> listOf(
                TorrentFile("p_1", "$cleanName.data", 1_450_000_000L, true),
                TorrentFile("p_2", "Documentation.pdf", 12_500_000L, true),
                TorrentFile("p_3", "checksums.sha256", 2_048L, true)
            )
        }

        val totalSize = sampleFiles.sumOf { it.sizeBytes }

        return ParsedTorrentInfo(
            name = cleanName,
            filePath = filePath,
            totalBytes = totalSize,
            files = sampleFiles
        )
    }

    override fun addTorrent(
        parsedInfo: ParsedTorrentInfo,
        destinationPath: String,
        selectedFileIds: Set<String>
    ) {
        val selectedFiles = parsedInfo.files.filter { it.id in selectedFileIds }
        val effectiveTotal = selectedFiles.sumOf { it.sizeBytes }.coerceAtLeast(1L)

        val newTorrent = Torrent(
            id = "torrent-${Random.nextInt(1000, 9999)}",
            name = parsedInfo.name,
            totalBytes = effectiveTotal,
            downloadedBytes = 0L,
            downloadSpeedBytesPerSec = 3_200_000L,
            uploadSpeedBytesPerSec = 120_000L,
            status = TorrentStatus.DOWNLOADING,
            destinationPath = destinationPath,
            files = selectedFiles
        )

        _torrents.value = listOf(newTorrent) + _torrents.value
    }

    override fun togglePlayPause(torrentId: String) {
        _torrents.value = _torrents.value.map { torrent ->
            if (torrent.id == torrentId) {
                when (torrent.status) {
                    TorrentStatus.DOWNLOADING -> torrent.copy(
                        status = TorrentStatus.PAUSED,
                        downloadSpeedBytesPerSec = 0L
                    )
                    TorrentStatus.PAUSED -> torrent.copy(
                        status = TorrentStatus.DOWNLOADING,
                        downloadSpeedBytesPerSec = 2_800_000L
                    )
                    else -> torrent
                }
            } else {
                torrent
            }
        }
    }

    override fun deleteTorrent(torrentId: String) {
        _torrents.value = _torrents.value.filterNot { it.id == torrentId }
    }
}
