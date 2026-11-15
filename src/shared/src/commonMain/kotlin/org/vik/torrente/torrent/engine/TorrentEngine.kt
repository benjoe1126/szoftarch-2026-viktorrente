package org.vik.torrente.torrent.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.BitSet
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread
import kotlin.random.Random

/**
 * Downloads a single torrent: finds peers via its UDP trackers, connects to them,
 * requests blocks, verifies completed pieces against their SHA-1 hashes and writes them to [downloadDir].
 *
 * Pieces are assigned to one peer at a time; each peer keeps up to [MAX_PIPELINED_REQUESTS]
 * block requests in flight for its current piece.
 */
class TorrentEngine(torrentFilePath: String, downloadDir: String = "downloads") {
    companion object {
        const val MAX_CONCURRENT_PEERS = 10
        const val BLOCK_SIZE = 16 * 1024 // 16 KiB standard block size
        const val MAX_PIPELINED_REQUESTS = 10
        const val LISTEN_PORT = 6881 // reported to trackers; we don't accept incoming connections yet
    }

    val metadata = TorrentMetadata(File(torrentFilePath).readBytes())
    val pieceLength: Int get() = metadata.pieceLength
    val numPieces: Int get() = metadata.numPieces
    val totalSize: Long get() = metadata.totalSize

    // Azureus-style peer id: client tag followed by random characters
    private val peerId: ByteArray = ("-VT0001-" + (1..12)
        .map { (('A'..'Z') + ('a'..'z') + ('0'..'9')).random() }
        .joinToString(""))
        .toByteArray(Charsets.US_ASCII)

    private val key: Int = Random.nextInt()

    // Stats reported to the tracker
    private val _downloaded = AtomicLong(0)
    private val _uploaded = AtomicLong(0)
    private val verifiedBytes = AtomicLong(0)

    /** Total bytes of piece data received from peers, including data that later failed verification. */
    val downloaded: Long get() = _downloaded.get()
    /** Bytes still needed to complete the torrent. */
    val left: Long get() = totalSize - verifiedBytes.get()
    /** Total bytes sent to peers. Always 0 for now, since we don't serve pieces. */
    val uploaded: Long get() = _uploaded.get()

    private val storage = PieceStorage(metadata, File(downloadDir))

    // All fields below are guarded by `lock`
    private val lock = Any()
    private val havePieces = BitSet(numPieces)
    private val assignedPieces = BitSet(numPieces) // pieces some peer is currently downloading
    private val sessions = mutableMapOf<PeerConnection, PeerSession>()
    private val pendingPeers = ArrayDeque<PeerAddresses>() // known peers we haven't tried yet
    private var connecting = 0 // connection attempts in progress
    private var downloading = false
    private var stopped = false

    private val executor: ExecutorService = Executors.newFixedThreadPool(MAX_CONCURRENT_PEERS) { runnable ->
        Thread(runnable, "PeerConnector").apply { isDaemon = true }
    }
    @Volatile private var lastTracker: String? = null // tracker that answered our last announce
    private val completedAnnounced = AtomicBoolean(false)

    /** Per-peer download state. */
    private class PeerSession(val connection: PeerConnection) {
        val pieces = BitSet()
        var peerChoking = true
        var amInterested = false
        var current: ActivePiece? = null
    }

    /** A piece being downloaded from a single peer. */
    private class ActivePiece(val index: Int, size: Int) {
        val data = ByteArray(size)
        var nextOffset = 0 // offset of the next block to request
        var inFlight = 0
        var received = 0
    }

    val isComplete: Boolean
        get() = synchronized(lock) { havePieces.cardinality() == numPieces }

    fun getDownloadProgress(): DownloadProgress = synchronized(lock) {
        DownloadProgress(
            piecesComplete = havePieces.cardinality(),
            piecesTotal = numPieces,
            bytesVerified = verifiedBytes.get(),
            totalSize = totalSize,
        )
    }

    fun getActiveConnections(): List<PeerConnection> = synchronized(lock) {
        sessions.keys.filter { it.isConnected }
    }

    /** Number of peers still queued or being connected to. */
    val pendingPeerCount: Int
        get() = synchronized(lock) { pendingPeers.size + connecting }

    // ------------------------------------------------------------------------
    // Trackers
    // ------------------------------------------------------------------------

    /**
     * Announces to the torrent's UDP trackers (in order) and returns the peers of the first one that answers.
     */
    suspend fun discoverPeers(): List<PeerAddresses> = withContext(Dispatchers.IO) {
        for (url in metadata.trackers.filter { it.startsWith("udp://") }) {
            try {
                val response = announce(url, AnnounceEvent.STARTED)
                println("[TorrentEngine] Tracker $url returned ${response.peerAddresses.size} peers (seeders=${response.seeders}, leechers=${response.leechers})")
                return@withContext response.peerAddresses.filter { it.address != 0 && it.portNumber != 0 }
            } catch (e: Exception) {
                println("[TorrentEngine] Tracker $url failed — ${e.message}")
            }
        }
        emptyList()
    }

    private fun announce(url: String, event: Int): AnnounceResponse =
        UdpTracker(url).announce(
            infoHash = metadata.infoHash,
            peerId = peerId,
            downloaded = downloaded,
            left = left,
            uploaded = uploaded,
            event = event,
            key = key,
            port = LISTEN_PORT,
        ).also { lastTracker = url }

    /** Fire-and-forget announce to the tracker that last answered. */
    private fun announceInBackground(event: Int) {
        val url = lastTracker ?: return
        thread(name = "Announce", isDaemon = true) {
            try {
                announce(url, event)
            } catch (e: Exception) {
                println("[TorrentEngine] Announce to $url failed — ${e.message}")
            }
        }
    }

    // ------------------------------------------------------------------------
    // Peers
    // ------------------------------------------------------------------------

    /**
     * Queues [peers] and connects to them in the background, keeping up to [MAX_CONCURRENT_PEERS]
     * connections open; unreachable or dropped peers are replaced by the next queued one.
     * Downloading from a peer begins once [startDownloading] was called and the peer unchokes us.
     */
    fun connectToPeers(peers: List<PeerAddresses>) {
        synchronized(lock) {
            pendingPeers.addAll(peers.filter { it !in pendingPeers })
        }
        fillConnectionSlots()
    }

    private fun fillConnectionSlots() = synchronized(lock) {
        while (!stopped && sessions.size + connecting < MAX_CONCURRENT_PEERS && pendingPeers.isNotEmpty()) {
            val peer = pendingPeers.removeFirst()
            connecting++
            executor.submit { tryConnect(peer) }
        }
    }

    private fun tryConnect(peer: PeerAddresses) {
        val connection = PeerConnection(
            host = peer.host,
            port = peer.portNumber,
            infoHash = metadata.infoHash,
            peerId = peerId,
            onMessage = ::handlePeerMessage,
            onDisconnect = ::handleDisconnect,
        )
        val connected = connection.connect()

        val accepted = synchronized(lock) {
            connecting--
            (connected && !stopped).also { if (it) sessions[connection] = PeerSession(connection) }
        }
        if (accepted) {
            connection.start()
            println("[TorrentEngine] Connected to peer $connection")
        } else {
            if (connected) connection.disconnect()
            fillConnectionSlots()
        }
    }

    /** Starts requesting pieces from all connected peers that have unchoked us. */
    fun startDownloading() {
        synchronized(lock) {
            if (downloading) return
            downloading = true
            println("[TorrentEngine] Starting download of $numPieces pieces ($totalSize bytes)")
            sessions.values.toList().forEach { requestBlocks(it) }
        }
    }

    /** Disconnects all peers and closes the downloaded files. */
    fun disconnectAll() {
        println("[TorrentEngine] Disconnecting all peers...")
        val connections = synchronized(lock) {
            downloading = false
            stopped = true
            pendingPeers.clear()
            sessions.keys.toList()
        }
        connections.forEach { it.disconnect() }
        executor.shutdownNow()
        storage.close()
        announceInBackground(AnnounceEvent.STOPPED)
    }

    private fun handleDisconnect(connection: PeerConnection, reason: String) {
        println("[TorrentEngine] $reason")
        synchronized(lock) {
            sessions.remove(connection)?.let { releaseCurrentPiece(it) }
        }
        fillConnectionSlots()
    }

    private fun handlePeerMessage(connection: PeerConnection, msg: PeerWireMessage) = synchronized(lock) {
        val session = sessions[connection] ?: return

        when (msg) {
            is PeerWireMessage.Bitfield -> {
                for (i in 0 until numPieces) {
                    if (msg.has(i)) session.pieces.set(i)
                }
                updateInterest(session)
                requestBlocks(session)
            }
            is PeerWireMessage.Have -> {
                session.pieces.set(msg.pieceIndex)
                updateInterest(session)
                requestBlocks(session)
            }
            is PeerWireMessage.Unchoke -> {
                session.peerChoking = false
                requestBlocks(session)
            }
            is PeerWireMessage.Choke -> {
                // A choking peer discards our pending requests, so give the piece back to the pool
                session.peerChoking = true
                releaseCurrentPiece(session)
            }
            is PeerWireMessage.Piece -> handleBlock(session, msg)
            else -> {} // We don't upload yet, so requests/interest from the peer are ignored
        }
    }

    private fun handleBlock(session: PeerSession, msg: PeerWireMessage.Piece) {
        val piece = session.current
        if (piece == null || piece.index != msg.pieceIndex || msg.begin + msg.block.size > piece.data.size) {
            return // stale block, e.g. from before a choke
        }

        _downloaded.addAndGet(msg.block.size.toLong())
        msg.block.copyInto(piece.data, msg.begin)
        piece.inFlight--
        piece.received += msg.block.size

        if (piece.received >= piece.data.size) {
            session.current = null
            assignedPieces.clear(piece.index)
            completePiece(piece)
        }
        requestBlocks(session)
    }

    private fun completePiece(piece: ActivePiece) {
        val hash = MessageDigest.getInstance("SHA-1").digest(piece.data)
        if (!hash.contentEquals(metadata.pieceHashes[piece.index])) {
            println("[TorrentEngine] Piece ${piece.index} failed hash check, will retry")
            return
        }

        storage.writePiece(piece.index, piece.data)
        havePieces.set(piece.index)
        verifiedBytes.addAndGet(piece.data.size.toLong())
        sessions.keys.toList().forEach { it.sendMessage(PeerWireMessage.Have(piece.index)) }

        if (havePieces.cardinality() == numPieces) {
            println("[TorrentEngine] Download complete")
            sessions.values.toList().forEach { updateInterest(it) }
            if (completedAnnounced.compareAndSet(false, true)) announceInBackground(AnnounceEvent.COMPLETED)
        }
    }

    /** Sends Interested/NotInterested when the peer gains or loses pieces we need. */
    private fun updateInterest(session: PeerSession) {
        val interested = hasNeededPiece(session)
        if (interested == session.amInterested) return
        session.amInterested = interested
        session.connection.sendMessage(if (interested) PeerWireMessage.Interested else PeerWireMessage.NotInterested)
    }

    private fun hasNeededPiece(session: PeerSession): Boolean {
        val needed = session.pieces.clone() as BitSet
        needed.andNot(havePieces)
        return !needed.isEmpty
    }

    /** Picks the first piece the peer has that nobody else is downloading. */
    private fun pickPiece(session: PeerSession): Int? {
        val candidates = session.pieces.clone() as BitSet
        candidates.andNot(havePieces)
        candidates.andNot(assignedPieces)
        return candidates.nextSetBit(0).takeIf { it in 0 until numPieces }
    }

    /** Keeps the peer's request pipeline full, assigning it a new piece when needed. */
    private fun requestBlocks(session: PeerSession) {
        if (!downloading || session.peerChoking || !session.connection.isConnected) return

        val piece = session.current ?: pickPiece(session)?.let { index ->
            assignedPieces.set(index)
            ActivePiece(index, metadata.pieceSize(index)).also { session.current = it }
        } ?: return

        while (piece.inFlight < MAX_PIPELINED_REQUESTS && piece.nextOffset < piece.data.size) {
            val length = minOf(BLOCK_SIZE, piece.data.size - piece.nextOffset)
            session.connection.sendMessage(PeerWireMessage.Request(piece.index, piece.nextOffset, length))
            piece.nextOffset += length
            piece.inFlight++
        }
    }

    /** Returns the session's piece to the pool and lets idle peers pick it up. */
    private fun releaseCurrentPiece(session: PeerSession) {
        val piece = session.current ?: return
        session.current = null
        assignedPieces.clear(piece.index)
        sessions.values.toList().forEach { requestBlocks(it) }
    }
}

data class DownloadProgress(
    val piecesComplete: Int,
    val piecesTotal: Int,
    val bytesVerified: Long,
    val totalSize: Long,
) {
    val percentage: Double
        get() = if (totalSize == 0L) 100.0 else bytesVerified * 100.0 / totalSize

    override fun toString() =
        "Progress: $piecesComplete/$piecesTotal pieces, $bytesVerified/$totalSize bytes (${"%.1f".format(percentage)}%)"
}
