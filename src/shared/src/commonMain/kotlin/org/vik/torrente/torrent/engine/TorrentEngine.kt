package org.vik.torrente.torrent.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.vik.torrente.repository.models.TorrentInfo
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.random.Random





class TorrentEngine(val torrent: TorrentInfo) {
    private fun discoverPeers(){
        
    }

    // connect establishes an udp connection between our client and the tracker, returns the response from the tracker
    private suspend fun connect(discoveryHost: String, discoveryPort: Int): ConnectResponse? = withContext(Dispatchers.IO) {
        DatagramSocket().use{ socket ->
            val address = InetAddress.getByName(discoveryHost)
            val connectRequest = ConnectRequest()
            val datagram = connectRequest.toBytes()
            val packet = DatagramPacket(
                datagram,
                datagram.size,
                address,
                discoveryPort
            )
            socket.send(packet)
            val buffer = ByteArray(16)
            val response = DatagramPacket(buffer, buffer.size)
            socket.soTimeout = 5000
            socket.receive(response)
            connectResponseFromBytes(response.data).let{
                if(it.transactionId == connectRequest.transactionId) {
                    it
                }else null
            }
        }
    }

    private suspend fun discoverPeersUDP(discoveryHost: String, discoveryPort: Int): Unit{

    }
}