package org.vik.torrente.repository.models

data class PeerId(
    val torrent: TorrentId,
    val idHash : String
)

data class PeerInfo(
    val torrent: TorrentId,
    val idHash : String,
    val address:String
)
{
    fun getId():PeerId{
        return PeerId(torrent, idHash)
    }
}