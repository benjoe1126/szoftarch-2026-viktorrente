package org.vik.torrente.torrent.engine

import org.vik.torrente.utils.asBytes
import org.vik.torrente.utils.asDictionary
import org.vik.torrente.utils.asList
import org.vik.torrente.utils.asLong
import org.vik.torrente.utils.asString
import org.vik.torrente.utils.bencodeDecode
import org.vik.torrente.utils.bencodeInfoHash

data class TorrentFile(
    val path: List<String>,
    val length: Long,
    val offset: Long, // byte offset of this file within the concatenated torrent data
)

/**
 * Parsed contents of a .torrent file (BEP-0003, plus announce-list from BEP-0012).
 */
class TorrentMetadata(torrentContent: ByteArray) {
    val infoHash: ByteArray = bencodeInfoHash(torrentContent)

    private val root = bencodeDecode(torrentContent)[0].asDictionary()
    private val info = root["info"]!!.asDictionary()

    val name: String = info["name"]?.asString() ?: "download"
    val pieceLength: Int = info["piece length"]!!.asLong().toInt()

    val pieceHashes: List<ByteArray> = info["pieces"]!!.asBytes().let { bytes ->
        require(bytes.size % 20 == 0) { "pieces length is not a multiple of 20" }
        (bytes.indices step 20).map { bytes.copyOfRange(it, it + 20) }
    }
    val numPieces: Int get() = pieceHashes.size

    /** Single-file torrents are represented as one file named after the torrent. */
    val files: List<TorrentFile> = run {
        val filesNode = info["files"]
            ?: return@run listOf(TorrentFile(listOf(name), info["length"]!!.asLong(), 0))
        var offset = 0L
        filesNode.asList().map { node ->
            val file = node.asDictionary()
            val length = file["length"]!!.asLong()
            TorrentFile(
                path = listOf(name) + file["path"]!!.asList().map { it.asString() },
                length = length,
                offset = offset,
            ).also { offset += length }
        }
    }

    val totalSize: Long = files.sumOf { it.length }

    /** All tracker URLs, primary announce first, without duplicates. */
    val trackers: List<String> = buildList {
        root["announce"]?.let { add(it.asString()) }
        root["announce-list"]?.asList()?.forEach { tier -> tier.asList().forEach { add(it.asString()) } }
    }.distinct()

    /** The last piece is usually shorter than [pieceLength]. */
    fun pieceSize(index: Int): Int =
        if (index == numPieces - 1) (totalSize - pieceLength.toLong() * index).toInt() else pieceLength
}
