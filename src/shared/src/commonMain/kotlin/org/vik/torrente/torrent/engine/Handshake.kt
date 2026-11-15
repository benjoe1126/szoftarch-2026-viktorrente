package org.vik.torrente.torrent.engine

import java.io.IOException
import java.nio.ByteBuffer

const val HANDSHAKE_SIZE = 68
private val PROTOCOL = "BitTorrent protocol".toByteArray(Charsets.US_ASCII)

class Handshake(
    val infoHash: ByteArray,
    val peerId: ByteArray,
    val reserved: ByteArray = ByteArray(8),
)

fun Handshake.toBytes(): ByteArray {
    val buffer = ByteBuffer.allocate(HANDSHAKE_SIZE)
    buffer.put(PROTOCOL.size.toByte())
    buffer.put(PROTOCOL)
    buffer.put(reserved)
    buffer.put(infoHash, 0, 20)
    buffer.put(peerId, 0, 20)
    return buffer.array()
}

fun handshakeFromBytes(bytes: ByteArray): Handshake {
    if (bytes.size < HANDSHAKE_SIZE) throw IOException("Handshake too short")
    val pstrLen = bytes[0].toInt() and 0xFF
    if (pstrLen != PROTOCOL.size || !bytes.copyOfRange(1, 1 + pstrLen).contentEquals(PROTOCOL)) {
        throw IOException("Invalid protocol string")
    }
    return Handshake(
        reserved = bytes.copyOfRange(20, 28),
        infoHash = bytes.copyOfRange(28, 48),
        peerId = bytes.copyOfRange(48, 68),
    )
}
