package org.vik.torrente.repository.models

enum class PieceStatus{
    PENDING_DOWNLOAD, DOWNLOADED, DELETED
}

data class PieceId(
    val torrent_id: TorrentId,
    val number: Long
)
{
    companion object{
        val PLACEHOLDER=PieceId(TorrentId.PLACEHOLDER,-1);
    }
}

data class PieceInfo(
    val id: PieceId,
    val hash:String,
    val length:Long,
    val number:Long,
    val status: PieceStatus,
    val downloadPath: String,
    val torrent: TorrentId
)