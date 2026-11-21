package org.vik.torrente.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.vik.torrente.model.Torrent
import org.vik.torrente.ui.util.Formatters

@Composable
fun TorrentDashboard(torrent: Torrent, modifier: Modifier = Modifier) {
    var selectedTabIndex by remember { mutableStateOf(2) } // default to peers
    val tabs = listOf("General", "Trackers", "Peers", "Content")

    Column(modifier = modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }
        
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (selectedTabIndex) {
                0 -> GeneralTab(torrent)
                1 -> TrackersTab(torrent)
                2 -> PeersTab(torrent)
                3 -> ContentTab(torrent)
            }
        }
    }
}

@Composable
fun GeneralTab(torrent: Torrent) {
    Column {
        Text("Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Save Path: ${torrent.destinationPath}")
        Text("Downloaded: ${Formatters.formatBytes(torrent.downloadedBytes)} / ${Formatters.formatBytes(torrent.totalBytes)}")
        Text("Status: ${torrent.status}")
        // can add more metadata here
    }
}

@Composable
fun TrackersTab(torrent: Torrent) {
    if (torrent.trackers.isEmpty()) {
        Text("No trackers configured.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Text("URL", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
                    Text("Status", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text("Peers", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()
            }
            items(torrent.trackers) { tracker ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(tracker.url, modifier = Modifier.weight(2f))
                    Text(tracker.status, modifier = Modifier.weight(1f))
                    Text(tracker.peers.toString(), modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun PeersTab(torrent: Torrent) {
    if (torrent.peerList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active peers.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("IP", modifier = Modifier.weight(2f), style = MaterialTheme.typography.labelMedium)
                Text("Client", modifier = Modifier.weight(2f), style = MaterialTheme.typography.labelMedium)
                Text("Flags", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                Text("%", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelMedium)
                Text("Down Speed", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelMedium)
                Text("Up Speed", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelMedium)
                Text("Downloaded", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelMedium)
                Text("Uploaded", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelMedium)
            }
            HorizontalDivider()
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(torrent.peerList, key = { it.ipAddress + it.port }) { peer ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(peer.ipAddress, modifier = Modifier.weight(2f), style = MaterialTheme.typography.bodySmall)
                        Text(peer.client, modifier = Modifier.weight(2f), style = MaterialTheme.typography.bodySmall)
                        Text(peer.flags, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = if (peer.progress >= 1.0f) "100.0" else "%.1f".format(peer.progress * 100f),
                            modifier = Modifier.weight(0.8f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = if (peer.downloadSpeed == 0L) "" else Formatters.formatSpeed(peer.downloadSpeed),
                            modifier = Modifier.weight(1.5f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = if (peer.uploadSpeed == 0L) "" else Formatters.formatSpeed(peer.uploadSpeed),
                            modifier = Modifier.weight(1.5f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(Formatters.formatBytes(peer.downloaded), modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodySmall)
                        Text(Formatters.formatBytes(peer.uploaded), modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun ContentTab(torrent: Torrent) {
    if (torrent.files.isEmpty()) {
        Text("No files in this torrent.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Text("File Path", modifier = Modifier.weight(3f), fontWeight = FontWeight.Bold)
                    Text("Size", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()
            }
            items(torrent.files) { file ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(file.path, modifier = Modifier.weight(3f))
                    Text(Formatters.formatBytes(file.sizeBytes), modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
