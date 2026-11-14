package org.vik.torrente.repository.models

data class TorrentId(
    val id: Long
)


data class TorrentInfo(
    val id: TorrentId,
    val tracker: String,
    val name: String,
    val length: Long,
    val files:List<FileId>,
    val pieces:List<PieceId>
)