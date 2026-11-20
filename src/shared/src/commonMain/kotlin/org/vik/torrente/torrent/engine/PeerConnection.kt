package org.vik.torrente.torrent.engine

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Manages a single TCP connection to a BitTorrent peer.
 * Handles the handshake exchange, reading/writing wire messages and periodic keep-alives.
 */
class PeerConnection(
    val host: String,
    val port: Int,
    private val infoHash: ByteArray,
    private val peerId: ByteArray,
    private val onMessage: (PeerConnection, PeerWireMessage) -> Unit = { _, _ -> },
    private val onDisconnect: (PeerConnection, String) -> Unit = { _, _ -> },
    private val socket: Socket = Socket(), // already connected for incoming connections, see [accept]
) {
    companion object {
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val KEEP_ALIVE_INTERVAL_MS = 60_000L // BEP-0003 suggests every 2 minutes at most
        private const val READ_TIMEOUT_MS = 150_000 // peers silent for longer than this are dead
        private const val MAX_MESSAGE_LENGTH = 1 shl 20
    }

    private lateinit var input: DataInputStream
    private lateinit var output: DataOutputStream
    private val writeLock = Any()
    private val running = AtomicBoolean(false)

    var remotePeerId: ByteArray = ByteArray(0)
        private set

    val isConnected: Boolean
        get() = running.get()

    override fun toString() = "$host:$port"

    /**
     * Connects to the peer and performs the handshake exchange.
     * Returns true if the connection was established successfully.
     */
    fun connect(): Boolean {
        return try {
            socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
            socket.soTimeout = READ_TIMEOUT_MS
            input = DataInputStream(socket.getInputStream().buffered())
            output = DataOutputStream(socket.getOutputStream().buffered())

            output.write(Handshake(infoHash, peerId).toBytes())
            output.flush()

            val response = ByteArray(HANDSHAKE_SIZE)
            input.readFully(response)
            val handshake = handshakeFromBytes(response)
            if (!handshake.infoHash.contentEquals(infoHash)) {
                throw IOException("Peer is serving a different torrent")
            }
            remotePeerId = handshake.peerId
            running.set(true)
            true
        } catch (e: Exception) {
            println("[PeerConnection] Failed to connect to $this — ${e.message}")
            closeSocket()
            false
        }
    }

    /**
     * Completes the handshake on an incoming connection whose handshake [PeerListener] already read,
     * by sending ours back. Returns true if the connection is ready for [start].
     */
    fun accept(remotePeerId: ByteArray): Boolean {
        return try {
            socket.soTimeout = READ_TIMEOUT_MS
            input = DataInputStream(socket.getInputStream().buffered())
            output = DataOutputStream(socket.getOutputStream().buffered())

            output.write(Handshake(infoHash, peerId).toBytes())
            output.flush()

            this.remotePeerId = remotePeerId
            running.set(true)
            true
        } catch (e: Exception) {
            println("[PeerConnection] Failed to accept $this — ${e.message}")
            closeSocket()
            false
        }
    }

    /** Starts the reader and keep-alive threads. Must be called after a successful [connect]. */
    fun start() {
        check(running.get()) { "Not connected" }

        thread(name = "Reader-$this", isDaemon = true) {
            try {
                while (running.get()) {
                    onMessage(this, readMessage())
                }
            } catch (e: Exception) {
                close("Connection to $this lost — ${e.message}")
            }
        }

        thread(name = "KeepAlive-$this", isDaemon = true) {
            try {
                while (running.get()) {
                    Thread.sleep(KEEP_ALIVE_INTERVAL_MS)
                    sendMessage(PeerWireMessage.KeepAlive)
                }
            } catch (_: InterruptedException) {
            }
        }
    }

    fun sendMessage(msg: PeerWireMessage) {
        if (!running.get()) return
        try {
            synchronized(writeLock) {
                output.write(msg.toBytes())
                output.flush()
            }
        } catch (e: IOException) {
            close("Send to $this failed — ${e.message}")
        }
    }

    /** Gracefully disconnect from the peer. */
    fun disconnect() = close("Disconnected from $this")

    private fun close(reason: String) {
        if (!running.compareAndSet(true, false)) return
        closeSocket()
        onDisconnect(this, reason)
    }

    private fun closeSocket() {
        try {
            socket.close()
        } catch (_: IOException) {
        }
    }

    private fun readMessage(): PeerWireMessage {
        val length = input.readInt()
        if (length == 0) return PeerWireMessage.KeepAlive
        if (length < 0 || length > MAX_MESSAGE_LENGTH) throw IOException("Invalid message length $length")

        val payload = ByteArray(length)
        input.readFully(payload)
        return PeerWireMessage.fromPayload(payload)
    }
}

// ============================================================================
// Wire Protocol Message Types (BEP-0003)
// ============================================================================

sealed class PeerWireMessage {
    object KeepAlive : PeerWireMessage()
    object Choke : PeerWireMessage()
    object Unchoke : PeerWireMessage()
    object Interested : PeerWireMessage()
    object NotInterested : PeerWireMessage()
    data class Have(val pieceIndex: Int) : PeerWireMessage()
    class Bitfield(val bits: ByteArray) : PeerWireMessage() {
        /** Bitfields are MSB-first: bit 7 of byte 0 is piece 0. */
        fun has(index: Int): Boolean {
            val byteIndex = index / 8
            if (byteIndex >= bits.size) return false
            return (bits[byteIndex].toInt() and (0x80 ushr (index % 8))) != 0
        }

        override fun toString() = "Bitfield(${bits.size} bytes)"
    }
    data class Request(val pieceIndex: Int, val begin: Int, val length: Int) : PeerWireMessage()
    class Piece(val pieceIndex: Int, val begin: Int, val block: ByteArray) : PeerWireMessage() {
        override fun toString() = "Piece(pieceIndex=$pieceIndex, begin=$begin, ${block.size} bytes)"
    }
    data class Cancel(val pieceIndex: Int, val begin: Int, val length: Int) : PeerWireMessage()
    data class Port(val port: Int) : PeerWireMessage()
    /** Extension or otherwise unsupported message, ignored by the engine. */
    data class Unknown(val id: Int) : PeerWireMessage()

    fun toBytes(): ByteArray {
        if (this is KeepAlive) return ByteArray(4)

        val (id, payload) = when (this) {
            is Choke -> 0 to ByteArray(0)
            is Unchoke -> 1 to ByteArray(0)
            is Interested -> 2 to ByteArray(0)
            is NotInterested -> 3 to ByteArray(0)
            is Have -> 4 to ByteBuffer.allocate(4).putInt(pieceIndex).array()
            is Bitfield -> 5 to bits
            is Request -> 6 to ByteBuffer.allocate(12).putInt(pieceIndex).putInt(begin).putInt(length).array()
            is Piece -> 7 to ByteBuffer.allocate(8 + block.size).putInt(pieceIndex).putInt(begin).put(block).array()
            is Cancel -> 8 to ByteBuffer.allocate(12).putInt(pieceIndex).putInt(begin).putInt(length).array()
            is Port -> 9 to ByteBuffer.allocate(2).putShort(port.toShort()).array()
            is Unknown, is KeepAlive -> throw IllegalStateException("Cannot encode $this")
        }
        return ByteBuffer.allocate(5 + payload.size)
            .putInt(1 + payload.size)
            .put(id.toByte())
            .put(payload)
            .array()
    }

    companion object {
        /** Decodes a message from its payload (message id followed by message body, length prefix stripped). */
        fun fromPayload(payload: ByteArray): PeerWireMessage {
            val buf = ByteBuffer.wrap(payload, 1, payload.size - 1)
            return when (val id = payload[0].toInt() and 0xFF) {
                0 -> Choke
                1 -> Unchoke
                2 -> Interested
                3 -> NotInterested
                4 -> Have(buf.int)
                5 -> Bitfield(payload.copyOfRange(1, payload.size))
                6 -> Request(buf.int, buf.int, buf.int)
                7 -> Piece(buf.int, buf.int, payload.copyOfRange(9, payload.size))
                8 -> Cancel(buf.int, buf.int, buf.int)
                9 -> Port(buf.short.toInt() and 0xFFFF)
                else -> Unknown(id)
            }
        }
    }
}
