package org.vik.torrente.repository.models

enum class PieceStatus{
    PENDING_DOWNLOAD, DOWNLOADED, DELETED
}

data class PieceId(
    val id: Long
)

data class PieceInfo(
    val id: PieceId,
    val hash:String,
    val length:Long,
    val number:Long,
    val status: PieceStatus,
    val downloadPath: String,
    val torrent: TorrentId,
    val peers:List<PeerId>
)