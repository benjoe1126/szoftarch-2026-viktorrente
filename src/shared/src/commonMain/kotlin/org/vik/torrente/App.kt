package org.vik.torrente

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.vik.torrente.model.ParsedTorrentInfo
import org.vik.torrente.platform.PlatformActions
import org.vik.torrente.service.MockTorrentService
import org.vik.torrente.service.TorrentService
import org.vik.torrente.ui.components.AddTorrentDialog
import org.vik.torrente.ui.components.TopBar
import org.vik.torrente.ui.components.TorrentList

@Composable
fun App(
    torrentService: TorrentService = remember {
        MockTorrentService(defaultDownloadPath = PlatformActions.getDefaultDownloadDirectory())
    }
) {
    // Stock Material 3 (Material You) baseline color schemes
    val colorScheme = if (isSystemInDarkTheme()) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }

    MaterialTheme(colorScheme = colorScheme) {
        val torrents by torrentService.torrents.collectAsState()
        val coroutineScope = rememberCoroutineScope()
        var pendingTorrent by remember { mutableStateOf<ParsedTorrentInfo?>(null) }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopBar(
                    torrents = torrents,
                    onAddTorrentClick = {
                        PlatformActions.chooseTorrentFile { filePath, fileName ->
                            coroutineScope.launch {
                                val parsed = torrentService.parseTorrent(filePath, fileName)
                                pendingTorrent = parsed
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            TorrentList(
                torrents = torrents,
                onTogglePlayPause = { id -> torrentService.togglePlayPause(id) },
                onDelete = { id -> torrentService.deleteTorrent(id) },
                onOpenFolder = { path -> PlatformActions.openDirectory(path) },
                onAddTorrentClick = {
                    PlatformActions.chooseTorrentFile { filePath, fileName ->
                        coroutineScope.launch {
                            val parsed = torrentService.parseTorrent(filePath, fileName)
                            pendingTorrent = parsed
                        }
                    }
                },
                modifier = Modifier.padding(innerPadding)
            )

            // Add Torrent Dialog (activated when a .torrent file is picked)
            pendingTorrent?.let { parsedInfo ->
                AddTorrentDialog(
                    parsedInfo = parsedInfo,
                    initialDestinationPath = PlatformActions.getDefaultDownloadDirectory(),
                    onConfirm = { destinationPath, selectedFileIds ->
                        torrentService.addTorrent(parsedInfo, destinationPath, selectedFileIds)
                        pendingTorrent = null
                    },
                    onDismiss = {
                        pendingTorrent = null
                    }
                )
            }
        }
    }
}
