package org.vik.torrente.torrent.engine

import java.io.File
import java.io.RandomAccessFile

/**
 * Writes verified pieces into the torrent's files under [downloadDir].
 * A piece may span several files, so each write is split along file boundaries.
 */
class PieceStorage(private val metadata: TorrentMetadata, downloadDir: File) : AutoCloseable {
    private val handles: List<RandomAccessFile> = metadata.files.map { file ->
        val target = File(downloadDir, file.path.joinToString(File.separator))
        target.parentFile?.mkdirs()
        RandomAccessFile(target, "rw").apply { setLength(file.length) }
    }

    @Synchronized
    fun writePiece(index: Int, data: ByteArray) {
        val pieceStart = index.toLong() * metadata.pieceLength
        val pieceEnd = pieceStart + data.size

        metadata.files.forEachIndexed { i, file ->
            val fileEnd = file.offset + file.length
            val start = maxOf(pieceStart, file.offset)
            val end = minOf(pieceEnd, fileEnd)
            if (start >= end) return@forEachIndexed

            handles[i].seek(start - file.offset)
            handles[i].write(data, (start - pieceStart).toInt(), (end - start).toInt())
        }
    }

    @Synchronized
    override fun close() {
        handles.forEach { it.close() }
    }
}
