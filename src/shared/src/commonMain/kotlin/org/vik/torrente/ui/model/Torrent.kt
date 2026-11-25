package org.vik.torrente.ui.model

import org.vik.torrente.repository.models.PeerStatus
import org.vik.torrente.repository.models.TorrentModeModel
import org.vik.torrente.repository.models.TorrentStatusModel
import java.sql.Time

/**
 * Represents a single file inside a multi-file or single-file torrent.
 */
data class TorrentFile(
    val id: String,
    val path: String,
    val sizeBytes: Long
)

data class Peer(
    val address: String,
    val status: PeerStatus,
)

/**
 * Represents a tracker for a torrent.
 */
data class Tracker(
    val url: String
)

/**
 * Represents an active or paused torrent task displayed in the UI.
 */
data class Torrent(
    val id: String,
    val name: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val downloadSpeedBytesPerSec: Long,
    val uploadSpeedBytesPerSec: Long,
    val lastModificationTime: Long = System.currentTimeMillis(),
    val status: TorrentStatusModel,
    val destinationPath: String,
    val mode: TorrentModeModel = TorrentModeModel.INACTIVE,
    val pieceCount: Int = 0,
    val peers: Int = 0,
    val peerList: List<Peer> = emptyList(),
    val trackers: List<Tracker> = emptyList(),
    val files: List<TorrentFile> = emptyList()
) {
    val progress: Float
        get() = if (totalBytes > 0L) {
            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val progressPercent: String
        get() = "${(progress * 100).toInt()}%"

    val isFinished: Boolean
        get() = progress >= 1.0f ||
            status == TorrentStatusModel.COMPLETED ||
            status == TorrentStatusModel.SEEDING
}

/**
 * Metadata extracted from a .torrent file before adding it to downloads.
 */
data class ParsedTorrentInfo(
    val name: String,
    val filePath: String,
    val totalBytes: Long,
    val files: List<TorrentFile>,
)
