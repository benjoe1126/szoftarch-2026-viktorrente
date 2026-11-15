package org.vik.torrente.torrent.engine

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.random.Random

data class ConnectRequest(
    val protocolID: Long = 0x41727101980,
    val action: Int = 0,
    val transactionId: Int = Random.nextInt()
)

fun ConnectRequest.toBytes(): ByteArray {
    val buffer = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN)
    buffer.putLong(protocolID)
    buffer.putInt(action)
    buffer.putInt(transactionId)
    return buffer.array()
}

data class ConnectResponse(
    val action: Int,
    val transactionId: Int,
    val connectionID: Long
)

fun connectResponseFromBytes(bytes: ByteArray): ConnectResponse {
    val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
    return ConnectResponse(
        action = buffer.int,
        transactionId = buffer.int,
        connectionID = buffer.long
    )
}
