package org.vik.torrente.service

import kotlinx.coroutines.flow.StateFlow
import org.vik.torrente.model.ParsedTorrentInfo
import org.vik.torrente.model.Torrent
import org.vik.torrente.model.TorrentMode

/**
 * Service interface for managing BitTorrent operations.
 * Replace [MockTorrentService] with the real engine implementation when ready.
 */
interface TorrentService {
    /**
     * Reactive stream of currently registered torrents.
     */
    val torrents: StateFlow<List<Torrent>>

    /**
     * Parses a .torrent file and returns its metadata and file list for selection.
     */
    suspend fun parseTorrent(filePath: String, fileName: String): ParsedTorrentInfo

    /**
     * Adds a parsed torrent to the active download queue with chosen destination.
     */
    fun addTorrent(
        parsedInfo: ParsedTorrentInfo,
        destinationPath: String,
        mode: TorrentMode
    )

    /**
     * Toggles between DOWNLOADING and PAUSED states.
     */
    fun togglePlayPause(torrentId: String)

    /**
     * Removes the torrent from the client.
     */
    fun deleteTorrent(torrentId: String)

    /**
     * Forces a re-announce to the tracker for a specific torrent.
     */
    fun reannounce(torrentId: String)

    /**
     * Manually adds a peer to a specific torrent.
     */
    fun addPeer(torrentId: String, peerIpAndPort: String)
}
