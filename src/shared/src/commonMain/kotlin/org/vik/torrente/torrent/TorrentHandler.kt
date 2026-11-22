package org.vik.torrente.torrent

import io.ktor.utils.io.locks.reentrantLock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import org.vik.torrente.torrent.engine.AnnounceEvent
import org.vik.torrente.torrent.engine.PeerAddresses
import org.vik.torrente.torrent.engine.TorrentEngine
import java.util.BitSet
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Runs one torrent session described by a [TorrentContext]: checks the data on disk, creates the [TorrentEngine],
 * finds and connects to peers, publishes progress on [TorrentContext.status] and reacts to [TorrentCommand]s
 * until it finishes or is stopped. A handler runs at most once; the scheduler creates a new one to restart a torrent.
 *
 * Pausing shuts the engine down; resuming starts a new one seeded with the pieces the old one had,
 * so no data is rechecked or downloaded again.
 */

data class TorrentSnapshot(
    val downloaded: Long,
    val uploaded: Long,
    val peers: List<PeerAddresses>,
    val numLeechers: Int,
    val numSeeders: Int
)

class TorrentHandler(private val context: TorrentContext) {
    companion object {
        private val STATUS_INTERVAL = 1.seconds
        private val REDISCOVER_INTERVAL = 30.seconds // how long to wait before asking trackers again when we have no peers
        private val REANNOUNCE_INTERVAL = 30.minutes // regular announce so trackers keep listing us
    }

    private val started = AtomicBoolean(false)

    // Only touched from the coroutine running [run]
    private var engine: TorrentEngine? = null // null while paused
    private var knownPieces: BitSet? = null // pieces we had when last paused
    private val extraPeers = linkedSetOf<PeerAddresses>() // from AddPeers, reconnected on resume
    private var lastAnnounce = TimeSource.Monotonic.markNow()
    private var completed = false
    // Counters of engines shut down by a pause, so the status shows totals
    private var previousDownloaded = 0L
    private var previousUploaded = 0L

    fun torrentSnapshot(): TorrentSnapshot? {
        return engine?.let {
            TorrentSnapshot(
                downloaded = it.downloaded,
                uploaded = it.uploaded,
                peers = it.peers,
                numLeechers = it.numLeechers,
                numSeeders = it.numSeeders
            )
        }
    }


    /** Launches [run] in [scope]. Cancelling the returned job stops the torrent with [StopReason.Cancelled]. */
    fun start(scope: CoroutineScope): Job = scope.launch(Dispatchers.IO) { run() }

    /** Runs the session until it completes (in [TorrentMode.DOWNLOAD]), is stopped, fails or is cancelled. */
    suspend fun run() {
        check(started.compareAndSet(false, true)) { "TorrentHandler can only be run once" }

        var reason: StopReason = StopReason.Cancelled
        try {
            reason = runSession()
        } catch (e: CancellationException) {
            reason = StopReason.Cancelled
            throw e
        } catch (e: Exception) {
            println("[TorrentHandler] ${context.torrentFilePath} failed — ${e.message}")
            reason = StopReason.Failed(e)
        } finally {
            withContext(NonCancellable) {
                stopEngine()
                publishStatus(TorrentPhase.STOPPED, reason)
                context.onStop(reason)
            }
        }
    }

    private suspend fun runSession(): StopReason {
        startEngine()

        while (true) {
            when (val command = nextCommand()) {
                TorrentCommand.Stop -> return StopReason.Requested
                TorrentCommand.Pause -> stopEngine()
                TorrentCommand.Resume -> if (engine == null) startEngine()
                TorrentCommand.Reannounce -> engine?.let { announce(it) }
                is TorrentCommand.AddPeers -> {
                    extraPeers += command.peers
                    engine?.connectToPeers(command.peers)
                }
                null -> {} // timeout, just refresh the status
            }

            val engine = engine
            if (engine == null) {
                publishStatus(TorrentPhase.PAUSED)
                continue
            }

            if (!completed && engine.isComplete) {
                completed = true
                context.onComplete(publishStatus(TorrentPhase.SEEDING))
                if (context.mode == TorrentMode.DOWNLOAD) return StopReason.Completed
            }

            val outOfPeers = engine.getActiveConnections().isEmpty() && engine.pendingPeerCount == 0
            val sinceAnnounce = lastAnnounce.elapsedNow()
            if (sinceAnnounce >= REANNOUNCE_INTERVAL || (outOfPeers && sinceAnnounce >= REDISCOVER_INTERVAL)) {
                announce(engine)
            }

            publishStatus(activePhase())
        }
    }

    private fun activePhase() =
        if (completed || context.mode == TorrentMode.SEED) TorrentPhase.SEEDING else TorrentPhase.DOWNLOADING

    /** Creates an engine, restores or checks the pieces on disk and connects to peers. */
    private suspend fun startEngine() = withContext(Dispatchers.IO) {
        val engine = TorrentEngine(context.torrentFilePath, context.downloadDir, context.peerListener)
        this@TorrentHandler.engine = engine // set right away so a failure below still shuts it down

        val pieces = knownPieces
        if (pieces != null) {
            engine.restorePieces(pieces)
        } else {
            publishStatus(TorrentPhase.CHECKING)
            engine.checkExistingPieces()
        }

        publishStatus(TorrentPhase.STARTING)
        engine.connectToPeers((engine.discoverPeers(AnnounceEvent.STARTED) + extraPeers).distinct())
        lastAnnounce = TimeSource.Monotonic.markNow()
        if (context.mode != TorrentMode.SEED) engine.startDownloading()
        publishStatus(activePhase())
    }

    /** Shuts the engine down, remembering its pieces and counters for a later [startEngine]. */
    private fun stopEngine() {
        val engine = engine ?: return
        knownPieces = engine.completedPieces
        previousDownloaded += engine.downloaded
        previousUploaded += engine.uploaded
        engine.disconnectAll()
        this.engine = null
    }

    private suspend fun announce(engine: TorrentEngine) {
        engine.connectToPeers(engine.discoverPeers(AnnounceEvent.NONE))
        lastAnnounce = TimeSource.Monotonic.markNow()
    }

    /** Waits for the next command, or returns null after [STATUS_INTERVAL]. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun nextCommand(): TorrentCommand? = select {
        context.commands.onReceive { it }
        onTimeout(STATUS_INTERVAL) { null }
    }

    /** Publishes the current status; while paused or stopped, keeps the last known progress. */
    private fun publishStatus(phase: TorrentPhase, stopReason: StopReason? = null): TorrentStatus {
        val engine = engine
        val status = if (engine == null) {
            context.status.value.copy(phase = phase, connectedPeers = 0, pendingPeers = 0, stopReason = stopReason)
        } else {
            TorrentStatus(
                phase = phase,
                progress = engine.getDownloadProgress(),
                connectedPeers = engine.getActiveConnections().size,
                pendingPeers = engine.pendingPeerCount,
                downloaded = previousDownloaded + engine.downloaded,
                uploaded = previousUploaded + engine.uploaded,
                stopReason = stopReason,
            )
        }
        return status.also(context::updateStatus)
    }
}
