package org.vik.torrente.model

/**
 * Status of a torrent in the client.
 */
enum class TorrentStatus {
    CHECKING,
    STARTING,
    DOWNLOADING,
    SEEDING,
    PAUSED,
    COMPLETED,
    STOPPED,
    ERROR
}

enum class TorrentMode {
    DOWNLOAD,
    DOWNLOAD_AND_SEED,
    SEED
}

/**
 * Represents a single file inside a multi-file or single-file torrent.
 */
data class TorrentFile(
    val id: String,
    val path: String,
    val sizeBytes: Long
)

/**
 * Represents a connected peer for a torrent.
 */
data class Peer(
    val ipAddress: String,
    val port: Int,
    val client: String,
    val downloadSpeed: Long, // bytes/sec
    val uploadSpeed: Long,   // bytes/sec
    val progress: Float      // 0.0 to 1.0
)

/**
 * Represents a tracker for a torrent.
 */
data class Tracker(
    val url: String,
    val status: String,
    val peers: Int
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
    val status: TorrentStatus,
    val destinationPath: String,
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
        get() = progress >= 1.0f || status == TorrentStatus.COMPLETED || status == TorrentStatus.SEEDING
}

/**
 * Metadata extracted from a .torrent file before adding it to downloads.
 */
data class ParsedTorrentInfo(
    val name: String,
    val filePath: String,
    val totalBytes: Long,
    val files: List<TorrentFile>
)
