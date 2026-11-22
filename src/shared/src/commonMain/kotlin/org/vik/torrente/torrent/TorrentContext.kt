package org.vik.torrente.torrent

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.vik.torrente.torrent.engine.DownloadProgress
import org.vik.torrente.torrent.engine.PeerAddresses
import org.vik.torrente.torrent.engine.PeerListener

/**
 * What a [TorrentHandler] should do with its torrent. In every mode, data already in the download
 * directory is checked first and valid pieces are kept, and pieces we have are served to peers.
 */
enum class TorrentMode {
    /** Download until every piece is verified, then stop. */
    DOWNLOAD,

    /** Download, then keep seeding until stopped. */
    DOWNLOAD_AND_SEED,

    /** Only serve the pieces already on disk, never download. */
    SEED,
}

/** Messages the scheduler can send to a running handler through [TorrentContext.send]. */
sealed interface TorrentCommand {
    /** Gracefully stop: disconnect from peers and announce `stopped` to the tracker. */
    data object Stop : TorrentCommand

    /** Disconnect from all peers but keep the session, so it can be resumed without rechecking the data. */
    data object Pause : TorrentCommand

    /** Reconnect after a [Pause]. */
    data object Resume : TorrentCommand

    /** Ask the trackers for peers again and connect to the new ones. */
    data object Reannounce : TorrentCommand

    /** Connect to peers learned from somewhere other than the trackers. */
    data class AddPeers(val peers: List<PeerAddresses>) : TorrentCommand
}

/** Why a handler stopped; passed to [TorrentContext.onStop]. */
sealed interface StopReason {
    /** All pieces were downloaded and the mode doesn't keep the torrent running. */
    data object Completed : StopReason

    /** A [TorrentCommand.Stop] was received. */
    data object Requested : StopReason

    /** The coroutine running the handler was cancelled. */
    data object Cancelled : StopReason

    data class Failed(val error: Throwable) : StopReason
}

enum class TorrentPhase {
    /** Hashing the data already on disk. */
    CHECKING,
    /** Contacting trackers and peers. */
    STARTING,
    DOWNLOADING,
    /** Serving pieces: after completion, or from the start in [TorrentMode.SEED]. */
    SEEDING,
    PAUSED,
    STOPPED,
}

/** Snapshot of a handler's state, published on [TorrentContext.status]. Byte counts are totals across pauses. */
data class TorrentStatus(
    val phase: TorrentPhase,
    val progress: DownloadProgress? = null,
    val connectedPeers: Int = 0,
    val pendingPeers: Int = 0,
    val downloaded: Long = 0,
    val uploaded: Long = 0,
    val stopReason: StopReason? = null,
)

/**
 * Everything a [TorrentHandler] needs from its scheduler, and the scheduler's line to the running handler.
 *
 * - Inbound: [send] / [stop] deliver [TorrentCommand]s to the handler (the Go-channel equivalent).
 * - Outbound: [status] is updated by the handler; [onComplete] and [onStop] are called by it.
 * - Hard stop: cancel the [kotlinx.coroutines.Job] returned by [TorrentHandler.start] (the `ctx.Done()` equivalent).
 *
 * [onComplete] is called once, when every piece is verified (immediately, if the data was already complete
 * on disk). [onStop] is called exactly once,
 * when the handler terminates for any reason (including after completion in [TorrentMode.DOWNLOAD]).
 * Both run on the handler's coroutine, so they should return quickly.
 *
 * [peerListener] lets peers connect to us, which seeding mostly relies on; the scheduler should create
 * one and share it between all handlers. Without it we only upload to peers we connected to ourselves.
 */
class TorrentContext(
    val torrentFilePath: String,
    val downloadDir: String = "downloads",
    val mode: TorrentMode = TorrentMode.DOWNLOAD,
    val onComplete: (TorrentContext, TorrentStatus) -> Unit = {_,_->},
    val onStop: (TorrentContext,StopReason) -> Unit = {_,_->},
    val peerListener: PeerListener? = null,
) {
    private val commandChannel = Channel<TorrentCommand>(Channel.UNLIMITED)
    internal val commands: ReceiveChannel<TorrentCommand> get() = commandChannel

    private val _status = MutableStateFlow(TorrentStatus(TorrentPhase.CHECKING))
    val status: StateFlow<TorrentStatus> = _status.asStateFlow()

    internal fun updateStatus(status: TorrentStatus) {
        _status.value = status
    }

    /** Queues [command] for the handler. Returns false only if the context can no longer accept commands. */
    fun send(command: TorrentCommand): Boolean = commandChannel.trySend(command).isSuccess

    fun stop(): Boolean = send(TorrentCommand.Stop)
}
