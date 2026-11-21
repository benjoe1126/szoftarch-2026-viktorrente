package org.vik.torrente.repository.models

enum class TorrentStatus {
    CHECKING,
    STARTING,
    DOWNLOADING,
    SEEDING,
    PAUSED,
    COMPLETED,
    STOPPED,
    ERROR
};

data class TorrentId(
    val hash : String
);

data class TorrentInfo(
    val hash: String,
    val tracker: String,
    val name: String,
    val length: Long,
    val status: TorrentStatus
)
{
    fun getId() : TorrentId {
        return TorrentId(hash);
    }
}