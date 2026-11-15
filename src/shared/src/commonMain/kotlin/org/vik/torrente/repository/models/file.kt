package org.vik.torrente.repository.models

data class FileId(
    val id: Long
)

data class FileInfo(
    val id: FileId,
    val name: String,
    val length: Long,
    val number: Long,
    val torrent: TorrentId,
)