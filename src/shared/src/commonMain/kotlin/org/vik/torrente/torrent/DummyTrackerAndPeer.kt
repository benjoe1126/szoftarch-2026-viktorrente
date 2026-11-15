package torrent

import org.vik.torrente.utils.bencodeEncode
import org.vik.torrente.utils.toHex
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import kotlin.random.Random

// ============================================================================
// Dummy UDP Tracker Server (BEP-0015)
// ============================================================================

class DummyTrackerServer(private val port: Int = 6969) : Thread("DummyTracker") {

    private val socket = DatagramSocket(port)

    // Store connection IDs so we can validate them on announce
    private val connectionIds = mutableSetOf<Long>()

    override fun run() {
        println("[Tracker] UDP tracker listening on port $port")
        val buffer = ByteArray(1024)

        while (!socket.isClosed) {
            try {
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)
                handlePacket(packet)
            } catch (_: IOException) {
                // socket closed by shutdown()
            }
        }
    }

    fun shutdown() = socket.close()

    private fun handlePacket(packet: DatagramPacket) {
        if (packet.length < 16) {
            println("[Tracker] Ignoring short packet (${packet.length} bytes)")
            return
        }

        val buf = ByteBuffer.wrap(packet.data, 0, packet.length).order(ByteOrder.BIG_ENDIAN)

        // Both CONNECT and ANNOUNCE have the action at offset 8 and the transaction id at offset 12
        val action = buf.getInt(8)
        val transactionId = buf.getInt(12)

        when (action) {
            0 -> handleConnect(packet, transactionId)
            1 -> handleAnnounce(packet, transactionId, buf)
            else -> println("[Tracker] Unknown action: $action")
        }
    }

    private fun handleConnect(packet: DatagramPacket, transactionId: Int) {
        val connectionId = Random.nextLong()
        connectionIds.add(connectionId)

        val response = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN).apply {
            putInt(0) // action: connect response
            putInt(transactionId)
            putLong(connectionId)
        }.array()
        socket.send(DatagramPacket(response, response.size, packet.address, packet.port))
    }

    private fun handleAnnounce(packet: DatagramPacket, transactionId: Int, buf: ByteBuffer) {
        val connectionId = buf.long
        if (!connectionIds.contains(connectionId)) {
            println("[Tracker] ANNOUNCE rejected: unknown connectionID=$connectionId")
            sendError(packet, transactionId, "Invalid connection ID")
            return
        }

        buf.position(16)
        val infoHash = ByteArray(20).also { buf.get(it) }
        val peerId = ByteArray(20).also { buf.get(it) }
        val downloaded = buf.long
        val left = buf.long
        val uploaded = buf.long
        val event = when (val e = buf.int) {
            0 -> "none"
            1 -> "completed"
            2 -> "started"
            3 -> "stopped"
            else -> "unknown($e)"
        }

        println("[Tracker] ANNOUNCE infoHash=${infoHash.toHex()}, peerID=${String(peerId, Charsets.US_ASCII)}, event=$event")
        println("[Tracker]   downloaded=$downloaded, uploaded=$uploaded, left=$left")

        // Reply with a single peer: the mock peer
        val addr = InetAddress.getByName(MockPeerServer.HOST).address
        val response = ByteBuffer.allocate(20 + 6).order(ByteOrder.BIG_ENDIAN).apply {
            putInt(1)  // action: announce response
            putInt(transactionId)
            putInt(30) // interval: 30 seconds
            putInt(0)  // leechers
            putInt(1)  // seeders
            put(addr)
            putShort(MockPeerServer.PORT.toShort())
        }.array()
        socket.send(DatagramPacket(response, response.size, packet.address, packet.port))
    }

    private fun sendError(packet: DatagramPacket, transactionId: Int, message: String) {
        val msgBytes = message.toByteArray(Charsets.UTF_8)
        val response = ByteBuffer.allocate(8 + msgBytes.size).order(ByteOrder.BIG_ENDIAN).apply {
            putInt(3) // action: error
            putInt(transactionId)
            put(msgBytes)
        }.array()
        socket.send(DatagramPacket(response, response.size, packet.address, packet.port))
    }
}

// ============================================================================
// Mock BitTorrent Peer (TCP Wire Protocol - BEP-0003)
// ============================================================================

/** A seeding peer that serves [data] split into pieces of [pieceLength]. */
class MockPeerServer(
    private val data: ByteArray,
    private val pieceLength: Int,
    private val port: Int = PORT,
) : Thread("MockPeer") {

    companion object {
        const val HOST = "127.0.0.1"
        const val PORT = 6881
    }

    private val numPieces = (data.size + pieceLength - 1) / pieceLength
    private val serverSocket = ServerSocket(port)

    override fun run() {
        println("[Peer] TCP peer listening on $HOST:$port")
        while (!serverSocket.isClosed) {
            try {
                val socket = serverSocket.accept()
                println("[Peer] Connection from ${socket.inetAddress.hostAddress}:${socket.port}")
                handlePeer(socket)
            } catch (_: IOException) {
                // socket closed by shutdown() or peer went away
            }
            println("[Peer] Connection closed")
        }
    }

    fun shutdown() = serverSocket.close()

    private fun handlePeer(socket: Socket) = socket.use { sock ->
        val input = DataInputStream(sock.getInputStream().buffered())
        val output = DataOutputStream(sock.getOutputStream().buffered())

        // 1. Read handshake: pstrlen(1) + pstr(19) + reserved(8) + info_hash(20) + peer_id(20)
        val handshake = ByteArray(68)
        input.readFully(handshake)
        val infoHash = handshake.copyOfRange(28, 48)
        val peerId = handshake.copyOfRange(48, 68)
        println("[Peer] Handshake: infoHash=${infoHash.toHex()}, peerID=${String(peerId, Charsets.US_ASCII)}")

        // 2. Send our handshake back with the same info hash
        output.write(handshake.copyOf(48) + "-MK0001-000000000000".toByteArray(Charsets.US_ASCII))

        // 3. Advertise all pieces, then unchoke
        val bitfield = ByteArray((numPieces + 7) / 8)
        for (i in 0 until numPieces) {
            bitfield[i / 8] = (bitfield[i / 8].toInt() or (0x80 ushr (i % 8))).toByte()
        }
        sendMessage(output, 5, bitfield)
        sendMessage(output, 1, ByteArray(0))
        output.flush()

        // 4. Answer requests until the client disconnects
        while (true) {
            val msgLen = input.readInt()
            if (msgLen == 0) continue // keep-alive
            val msgId = input.readUnsignedByte()
            val payload = ByteArray(msgLen - 1).also { input.readFully(it) }

            if (msgId != 6) continue // only requests need an answer
            val req = ByteBuffer.wrap(payload)
            val pieceIndex = req.int
            val begin = req.int
            val length = req.int

            val offset = pieceIndex.toLong() * pieceLength + begin
            if (offset < 0 || offset + length > data.size) {
                println("[Peer] Ignoring out of range request piece=$pieceIndex, begin=$begin, length=$length")
                continue
            }
            val block = ByteBuffer.allocate(8 + length).putInt(pieceIndex).putInt(begin)
                .put(data, offset.toInt(), length).array()
            sendMessage(output, 7, block)
            output.flush()
        }
    }

    private fun sendMessage(output: DataOutputStream, msgId: Int, payload: ByteArray) {
        output.writeInt(1 + payload.size)
        output.writeByte(msgId)
        output.write(payload)
    }
}

// ============================================================================
// Test torrent + convenience functions for Main.kt
// ============================================================================

/**
 * Reads every file under [dir] as the content the mock peer will seed, keyed by its
 * '/'-separated path relative to [dir]. Files are sorted so the torrent layout is stable.
 */
fun loadSeedFiles(dir: File): Map<String, ByteArray> =
    dir.walk()
        .filter { it.isFile }
        .sortedBy { it.relativeTo(dir).invariantSeparatorsPath }
        .associate { it.relativeTo(dir).invariantSeparatorsPath to it.readBytes() }

/**
 * Writes a multi-file .torrent describing [files] ('/'-separated path -> contents) concatenated together,
 * announcing to the dummy tracker on [trackerPort].
 */
fun createDummyTorrent(target: File, name: String, files: Map<String, ByteArray>, pieceLength: Int, trackerPort: Int): File {
    val data = files.values.fold(ByteArray(0)) { acc, bytes -> acc + bytes }
    val sha1 = MessageDigest.getInstance("SHA-1")
    val pieces = (data.indices step pieceLength)
        .map { sha1.digest(data.copyOfRange(it, minOf(it + pieceLength, data.size))) }
        .fold(ByteArray(0)) { acc, hash -> acc + hash }

    val torrent = mapOf(
        "announce" to "udp://127.0.0.1:$trackerPort/announce",
        "info" to mapOf(
            "name" to name,
            "piece length" to pieceLength,
            "pieces" to pieces,
            "files" to files.map { (path, bytes) -> mapOf("length" to bytes.size, "path" to path.split("/")) },
        ),
    )
    target.parentFile?.mkdirs()
    target.writeBytes(bencodeEncode(torrent))
    return target
}

data class DummyServices(val tracker: DummyTrackerServer, val peer: MockPeerServer)

/**
 * Starts the dummy tracker (UDP) and a mock peer (TCP) seeding [data] as daemon threads.
 */
fun startDummyTrackerAndPeer(data: ByteArray, pieceLength: Int, trackerPort: Int = 6969): DummyServices {
    val tracker = DummyTrackerServer(port = trackerPort).apply { isDaemon = true; start() }
    val peer = MockPeerServer(data, pieceLength).apply { isDaemon = true; start() }
    return DummyServices(tracker, peer)
}

fun stopDummyServices(services: DummyServices) {
    services.tracker.shutdown()
    services.peer.shutdown()
    services.tracker.join(2000)
    services.peer.join(2000)
}
