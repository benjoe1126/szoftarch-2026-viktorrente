package org.vik.torrente.torrent.engine

import org.vik.torrente.utils.toHex
import java.io.DataInputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/**
 * Accepts incoming peer connections on [port] and hands each one to the engine serving the torrent
 * named in the peer's handshake. One listener is shared by all engines; pass 0 to pick a free port.
 */
class PeerListener(port: Int = TorrentEngine.LISTEN_PORT) : AutoCloseable {
    companion object {
        private const val HANDSHAKE_TIMEOUT_MS = 10_000
    }

    /** Called with the accepted socket and the remote handshake; our handshake hasn't been sent yet. */
    fun interface IncomingHandler {
        fun onIncoming(socket: Socket, handshake: Handshake)
    }

    private val serverSocket = ServerSocket(port)
    private val torrents = ConcurrentHashMap<String, IncomingHandler>() // keyed by hex info hash

    /** The port we actually listen on, reported to trackers. */
    val port: Int get() = serverSocket.localPort

    init {
        thread(name = "PeerListener", isDaemon = true) {
            while (!serverSocket.isClosed) {
                try {
                    val socket = serverSocket.accept()
                    thread(name = "Incoming-${socket.inetAddress.hostAddress}", isDaemon = true) { dispatch(socket) }
                } catch (_: IOException) {
                    // socket closed by close()
                }
            }
        }
    }

    /** Routes connections for [infoHash] to [handler]. Only one engine per torrent can listen; later ones are ignored. */
    fun register(infoHash: ByteArray, handler: IncomingHandler) {
        if (torrents.putIfAbsent(infoHash.toHex(), handler) != null) {
            println("[PeerListener] ${infoHash.toHex()} already has a listening engine, not accepting incoming peers for another")
        }
    }

    /** Removes [handler]'s registration; does nothing if another engine owns [infoHash]. */
    fun unregister(infoHash: ByteArray, handler: IncomingHandler) {
        torrents.remove(infoHash.toHex(), handler)
    }

    private fun dispatch(socket: Socket) {
        try {
            socket.soTimeout = HANDSHAKE_TIMEOUT_MS
            val bytes = ByteArray(HANDSHAKE_SIZE)
            DataInputStream(socket.getInputStream()).readFully(bytes)
            val handshake = handshakeFromBytes(bytes)
            val handler = torrents[handshake.infoHash.toHex()] ?: throw IOException("Unknown torrent")
            handler.onIncoming(socket, handshake)
        } catch (e: Exception) {
            println("[PeerListener] Rejected ${socket.inetAddress.hostAddress} — ${e.message}")
            socket.close()
        }
    }

    override fun close() = serverSocket.close()
}
