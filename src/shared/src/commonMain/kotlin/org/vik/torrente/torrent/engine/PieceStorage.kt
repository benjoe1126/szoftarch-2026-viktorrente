package org.vik.torrente.torrent.engine

import java.io.File
import java.io.RandomAccessFile

/**
 * Reads and writes pieces in the torrent's files under [downloadDir].
 * A piece may span several files, so each access is split along file boundaries.
 */
class PieceStorage(private val metadata: TorrentMetadata, downloadDir: File) : AutoCloseable {
    // Whether each file was already on disk before we opened it, i.e. may hold data from an earlier session
    private val preexisting = BooleanArray(metadata.files.size)

    private val handles: List<RandomAccessFile> = metadata.files.mapIndexed { i, file ->
        val target = File(downloadDir, file.path.joinToString(File.separator))
        preexisting[i] = target.isFile && target.length() > 0
        target.parentFile?.mkdirs()
        RandomAccessFile(target, "rw").apply { setLength(file.length) }
    }

    @Synchronized
    fun writePiece(index: Int, data: ByteArray) {
        forEachSpan(index.toLong() * metadata.pieceLength, data.size) { handle, bufferOffset, length ->
            handle.write(data, bufferOffset, length)
        }
    }

    /** Reads [length] bytes starting at [begin] within piece [index]. */
    @Synchronized
    fun readBlock(index: Int, begin: Int, length: Int): ByteArray {
        val data = ByteArray(length)
        forEachSpan(index.toLong() * metadata.pieceLength + begin, length) { handle, bufferOffset, spanLength ->
            handle.readFully(data, bufferOffset, spanLength)
        }
        return data
    }

    /** False if every file the piece touches was freshly created, so it can't hold valid data yet. */
    fun mayContain(index: Int): Boolean {
        val pieceStart = index.toLong() * metadata.pieceLength
        val pieceEnd = pieceStart + metadata.pieceSize(index)
        return metadata.files.indices.any { i ->
            val file = metadata.files[i]
            preexisting[i] && file.offset < pieceEnd && file.offset + file.length > pieceStart
        }
    }

    /** Calls [action] for each file region covering [size] bytes from torrent offset [start], with the file positioned. */
    private fun forEachSpan(start: Long, size: Int, action: (RandomAccessFile, Int, Int) -> Unit) {
        val end = start + size
        metadata.files.forEachIndexed { i, file ->
            val spanStart = maxOf(start, file.offset)
            val spanEnd = minOf(end, file.offset + file.length)
            if (spanStart >= spanEnd) return@forEachIndexed

            handles[i].seek(spanStart - file.offset)
            action(handles[i], (spanStart - start).toInt(), (spanEnd - spanStart).toInt())
        }
    }

    @Synchronized
    override fun close() {
        handles.forEach { it.close() }
    }
}
