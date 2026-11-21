package org.vik.torrente.repository.models

enum class PeerStatus{
    SEEDING,
    LEECHING,
    SEEDING_AND_LEECHING,
    INACTIVE,
}

data class PeerId(
    val torrent: TorrentId,
    val idHash : String
)

data class PeerInfo(
    val torrent: TorrentId,
    val idHash : String,
    val address:String,
    val status:PeerStatus
)
{
    fun getId():PeerId{
        return PeerId(torrent, idHash)
    }
}