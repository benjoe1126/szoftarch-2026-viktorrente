package org.vik.torrente

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.vik.torrente.torrent.StopReason
import org.vik.torrente.torrent.TorrentCommand
import org.vik.torrente.torrent.TorrentContext
import org.vik.torrente.torrent.TorrentHandler
import org.vik.torrente.torrent.TorrentMode
import org.vik.torrente.torrent.TorrentPhase
import org.vik.torrente.torrent.engine.PeerAddresses
import org.vik.torrente.torrent.engine.PeerListener
import torrent.DummyTrackerServer
import torrent.createDummyTorrent
import torrent.startDummyTrackerAndPeer
import torrent.stopDummyServices
import java.io.File
import java.nio.file.Files
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TorrentHandlerTest {
    private val pieceLength = 32 * 1024
    private val dir: File = Files.createTempDirectory("torrent-handler").toFile()
    private val files = mapOf("a.bin" to Random.nextBytes(100_000), "sub/b.bin" to Random.nextBytes(50_000))
    private val data = files.values.fold(ByteArray(0)) { acc, bytes -> acc + bytes }

    private fun assertDownloaded(downloadDir: File) = files.forEach { (path, bytes) ->
        assertContentEquals(bytes, downloadDir.resolve("t/$path").readBytes(), path)
    }

    private fun localPeer(port: Int) = PeerAddresses(0x7F000001, port.toShort())

    @Test
    fun downloadsFromMockPeer() = runBlocking {
        val torrent = createDummyTorrent(dir.resolve("t.torrent"), "t", files, pieceLength, 6970)
        val services = startDummyTrackerAndPeer(data, pieceLength, 6970)
        try {
            val completed = CompletableDeferred<Unit>()
            val stopped = CompletableDeferred<StopReason>()
            val ctx = TorrentContext(
                torrent.path, dir.resolve("out").path, TorrentMode.DOWNLOAD,
                onComplete = { completed.complete(Unit) },
                onStop = { stopped.complete(it) },
            )
            TorrentHandler(ctx).start(this)
            assertEquals(StopReason.Completed, withTimeout(30_000) { stopped.await() })
            assertTrue(completed.isCompleted)
            assertDownloaded(dir.resolve("out"))

            // Same directory again: the data is found on disk, nothing is downloaded
            val stoppedAgain = CompletableDeferred<StopReason>()
            val again = TorrentContext(torrent.path, dir.resolve("out").path, onStop = { stoppedAgain.complete(it) })
            TorrentHandler(again).start(this)
            assertEquals(StopReason.Completed, withTimeout(30_000) { stoppedAgain.await() })
            assertEquals(0, again.status.value.downloaded)
        } finally {
            stopDummyServices(services)
        }
    }

    @Test
    fun seedsToAnotherHandlerAndSurvivesPause() = runBlocking {
        // The tracker only knows the (not running) mock peer, so peers are handed over with AddPeers
        val torrent = createDummyTorrent(dir.resolve("t.torrent"), "t", files, pieceLength, 6971)
        val tracker = DummyTrackerServer(6971).apply { isDaemon = true; start() }
        val listener = PeerListener(0)
        try {
            files.forEach { (path, bytes) ->
                dir.resolve("seed/t/$path").apply { parentFile.mkdirs() }.writeBytes(bytes)
            }
            val seeder = TorrentContext(torrent.path, dir.resolve("seed").path, TorrentMode.SEED, peerListener = listener)
            val seederJob = TorrentHandler(seeder).start(this)
            withTimeout(30_000) { seeder.status.first { it.phase == TorrentPhase.SEEDING } }

            val stopped = CompletableDeferred<StopReason>()
            val leecher = TorrentContext(
                torrent.path, dir.resolve("leech").path, TorrentMode.DOWNLOAD_AND_SEED,
                onStop = { stopped.complete(it) },
            )
            TorrentHandler(leecher).start(this)

            leecher.send(TorrentCommand.Pause)
            withTimeout(30_000) { leecher.status.first { it.phase == TorrentPhase.PAUSED } }
            leecher.send(TorrentCommand.AddPeers(listOf(localPeer(listener.port)))) // remembered while paused
            leecher.send(TorrentCommand.Resume)

            withTimeout(30_000) { leecher.status.first { it.phase == TorrentPhase.SEEDING } }
            assertDownloaded(dir.resolve("leech"))
            withTimeout(10_000) { seeder.status.first { it.uploaded == data.size.toLong() } }

            leecher.stop()
            assertEquals(StopReason.Requested, withTimeout(10_000) { stopped.await() })
            seederJob.cancel()
            seederJob.join()
            assertEquals(TorrentPhase.STOPPED, seeder.status.value.phase)
            assertEquals(StopReason.Cancelled, seeder.status.value.stopReason)
        } finally {
            listener.close()
            tracker.shutdown()
        }
    }
}
