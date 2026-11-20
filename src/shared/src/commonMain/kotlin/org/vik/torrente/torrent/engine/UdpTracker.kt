package org.vik.torrente.torrent.engine

import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.URI
import java.nio.ByteBuffer

/**
 * Minimal UDP tracker client (BEP-0015). Each announce performs a fresh connect,
 * since connection IDs are only valid for a minute anyway.
 */
class UdpTracker(val url: String) {
    companion object {
        private const val TIMEOUT_MS = 5_000
        private const val ACTION_ERROR = 3
    }

    private val address: InetSocketAddress = URI(url).let { uri ->
        require(uri.scheme == "udp") { "Not a UDP tracker: $url" }
        InetSocketAddress(uri.host, uri.port)
    }

    fun announce(
        infoHash: ByteArray,
        peerId: ByteArray,
        downloaded: Long,
        left: Long,
        uploaded: Long,
        event: Int,
        key: Int,
        port: Int,
    ): AnnounceResponse = DatagramSocket().use { socket ->
        socket.soTimeout = TIMEOUT_MS

        val connectRequest = ConnectRequest()
        val connectReply = exchange(socket, connectRequest.toBytes(), 16)
        val connectResponse = connectResponseFromBytes(connectReply)
        if (connectResponse.transactionId != connectRequest.transactionId) {
            throw IOException("Tracker $url: connect transaction id mismatch")
        }

        val request = AnnounceRequest(
            connectionId = connectResponse.connectionID,
            infoHash = infoHash,
            peerId = peerId,
            downloaded = downloaded,
            left = left,
            uploaded = uploaded,
            event = event,
            key = key,
            port = port,
        )
        val reply = exchange(socket, request.toBytes(), 20)
        val response = announceResponseFromBytes(reply)
        if (response.transactionId != request.transactionId) {
            throw IOException("Tracker $url: announce transaction id mismatch")
        }
        response
    }

    /** Sends a request and returns the reply, throwing if the tracker answered with an error. */
    private fun exchange(socket: DatagramSocket, request: ByteArray, minReplySize: Int): ByteArray {
        socket.send(DatagramPacket(request, request.size, address))
        val buffer = ByteArray(65_507)
        val packet = DatagramPacket(buffer, buffer.size)
        socket.receive(packet)

        val reply = buffer.copyOf(packet.length)
        if (reply.size >= 8 && ByteBuffer.wrap(reply).int == ACTION_ERROR) {
            throw IOException("Tracker $url error: ${String(reply, 8, reply.size - 8, Charsets.UTF_8)}")
        }
        if (reply.size < minReplySize) throw IOException("Tracker $url: reply too short (${reply.size} bytes)")
        return reply
    }
}
