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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.vik.torrente.ui.model.Torrent
import org.vik.torrente.ui.util.Formatters

import org.vik.torrente.ui.icons.AppIcons

@Composable
fun TorrentDashboard(torrent: Torrent, onClose: () -> Unit, modifier: Modifier = Modifier) {
    var selectedTabIndex by remember { mutableStateOf(2) } // default to peers
    val tabs = listOf("General", "Trackers", "Peers", "Content")

    Column(modifier = modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                divider = {},
                modifier = Modifier.padding(end = 48.dp) // Leave space for the close button
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = AppIcons.Close,
                    contentDescription = "Close details",
                    modifier = Modifier.size(24.dp)
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
        Text("Pieces: ${torrent.pieceCount}")
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
                    Text("URL", fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()
            }
            items(torrent.trackers) { tracker ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(tracker.url)
                }
            }
        }
    }
}

@Composable
fun PeersTab(torrent: Torrent) {
    if (torrent.peerList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No peer addresses available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("Address", style = MaterialTheme.typography.labelMedium)
            }
            HorizontalDivider()
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(torrent.peerList, key = { it.address }) { peer ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(peer.address, style = MaterialTheme.typography.bodySmall)
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
