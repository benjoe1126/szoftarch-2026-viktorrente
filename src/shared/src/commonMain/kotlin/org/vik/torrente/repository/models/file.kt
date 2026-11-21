package org.vik.torrente.repository.models

data class FileId(
    val torrent: TorrentId,
    val number: Long
)

data class FileInfo(
    val torrent: TorrentId,
    val number: Long,
    val name: String,
    val length: Long
)
{
    fun getId():FileId{
        return FileId(torrent, number);
    }
}