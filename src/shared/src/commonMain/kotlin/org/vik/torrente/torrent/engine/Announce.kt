package org.vik.torrente.torrent.engine

import java.nio.ByteBuffer
import java.nio.CharBuffer
import kotlin.random.Random

data class AnnounceRequest (
    val connectionId: Long,
    val action : Int = 1,
    val transactionId : Int = Random.nextInt(),
    val infoHash: String,
    val peerId: String,
    val downloaded: Long,
    val left: Long,
    val uploaded: Long,
    val event: Int = 0, // 0: none, 1: compelted, 2: started, 3: stopped
    val ipAddress: Int = 0,
    val key: Int,
    val numWant: Int = -1,
    val port: Short
)

data class PeerAddresses(
    val address: Int,
    val port: Short,
)

data class AnnounceResponse(
    val action: Int = 1,
    val transactionId: Int,
    val intervals: Int,
    val leechers: Int,
    val seeders: Int,
    val peerAddresses: List<PeerAddresses>,
)