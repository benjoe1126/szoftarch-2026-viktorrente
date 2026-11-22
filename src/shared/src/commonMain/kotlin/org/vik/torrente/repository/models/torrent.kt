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

/*
ViewModel instructions:
[INACTIVE]: not yet started or marked to stop
[DOWNLOADING,SEEDING,DOWNLOADING_AND_SEEDING]: active or marked to start
[PAUSED]: paused or marked to pause
 */
enum class TorrentModeModel{
    INACTIVE,
    DOWNLOADING,
    SEEDING,
    DOWNLOADING_AND_SEEDING,
    PAUSED
}

data class TorrentId(
    val hash : String
);

data class TorrentInfo(
    val hash: String,
    val tracker: String,
    val name: String,
    val length: Long,
    val status: TorrentStatus,
    val mode: TorrentModeModel,
    val filePath:String
)
{
    fun getId() : TorrentId {
        return TorrentId(hash);
    }
}