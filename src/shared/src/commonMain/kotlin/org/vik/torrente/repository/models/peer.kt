package org.vik.torrente.repository.models

data class PeerId(
    val id: Long
)

data class PeerInfo(
    val id:PeerId,
    val address:String,
    val torrent:TorrentId,
    val piecesOffered:List<PieceId>
)