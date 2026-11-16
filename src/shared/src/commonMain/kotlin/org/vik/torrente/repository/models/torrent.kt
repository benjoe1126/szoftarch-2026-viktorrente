package org.vik.torrente.repository.models

data class TorrentId(
    val id: Long
)
{
    companion object{
        val PLACEHOLDER = TorrentId(-1);
    }
}


data class TorrentInfo(
    val id: TorrentId,
    val tracker: String,
    val name: String,
    val length: Long
)