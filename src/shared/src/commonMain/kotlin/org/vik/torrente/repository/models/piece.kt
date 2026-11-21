package org.vik.torrente.repository.models

enum class PieceStatus{
    AVAILABLE, NOT_AVAILABLE, DOWNLOADING
}

data class PieceId(
    val torrent: TorrentId,
    val number : Long
)

data class PieceInfo(
    val torrent: TorrentId,
    val number:Long,
    val hash:String,
    val length:Long,
    val status: PieceStatus,
    val downloadPath: String
)
{
    fun getId() : PieceId{
        return PieceId(torrent, number)
    }
}