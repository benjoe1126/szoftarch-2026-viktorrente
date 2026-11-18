package org.vik.torrente.repository.models

data class PeerId(
    val torrent_id: TorrentId,
    val address: String
)
{
    companion object{
        val PLACEHOLDER = PeerId(TorrentId.PLACEHOLDER,"109.71.161.200");
    }
}

data class PeerInfo(
    val id: PeerId,
    val address:String,
    val torrent:TorrentId
)