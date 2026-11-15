package org.vik.torrente.torrent.engine

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.random.Random

// BEP-0015 announce event values
object AnnounceEvent {
    const val NONE = 0
    const val COMPLETED = 1
    const val STARTED = 2
    const val STOPPED = 3
}

class AnnounceRequest(
    val connectionId: Long,
    val transactionId: Int = Random.nextInt(),
    val infoHash: ByteArray,
    val peerId: ByteArray,
    val downloaded: Long,
    val left: Long,
    val uploaded: Long,
    val event: Int = AnnounceEvent.NONE,
    val ipAddress: Int = 0, // let the tracker determine it
    val key: Int,
    val numWant: Int = -1,
    val port: Int,
)

fun AnnounceRequest.toBytes(): ByteArray {
    val buffer = ByteBuffer.allocate(98).order(ByteOrder.BIG_ENDIAN)
    buffer.putLong(connectionId)
    buffer.putInt(1) // action: announce
    buffer.putInt(transactionId)
    buffer.put(infoHash, 0, 20)
    buffer.put(peerId, 0, 20)
    buffer.putLong(downloaded)
    buffer.putLong(left)
    buffer.putLong(uploaded)
    buffer.putInt(event)
    buffer.putInt(ipAddress)
    buffer.putInt(key)
    buffer.putInt(numWant)
    buffer.putShort(port.toShort())
    return buffer.array()
}

data class PeerAddresses(
    val address: Int,
    val port: Short,
) {
    val host: String
        get() = "${address shr 24 and 0xFF}.${address shr 16 and 0xFF}.${address shr 8 and 0xFF}.${address and 0xFF}"

    // ports above 32767 come through as negative shorts
    val portNumber: Int
        get() = port.toInt() and 0xFFFF
}

fun PeerAddresses.toDecimalString() = "$host:$portNumber"

data class AnnounceResponse(
    val transactionId: Int,
    val interval: Int,
    val leechers: Int,
    val seeders: Int,
    val peerAddresses: List<PeerAddresses>,
)

fun announceResponseFromBytes(bytes: ByteArray, length: Int = bytes.size): AnnounceResponse {
    val buffer = ByteBuffer.wrap(bytes, 0, length).order(ByteOrder.BIG_ENDIAN)
    buffer.int // action
    val transactionId = buffer.int
    val interval = buffer.int
    val leechers = buffer.int
    val seeders = buffer.int

    val peerAddresses = buildList {
        while (buffer.remaining() >= 6) {
            add(PeerAddresses(buffer.int, buffer.short))
        }
    }

    return AnnounceResponse(
        transactionId = transactionId,
        interval = interval,
        leechers = leechers,
        seeders = seeders,
        peerAddresses = peerAddresses,
    )
}
