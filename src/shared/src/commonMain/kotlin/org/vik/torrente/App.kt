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
import org.vik.torrente.service.MockTorrentService
import org.vik.torrente.service.TorrentService
import org.vik.torrente.ui.components.AddTorrentDialog
import org.vik.torrente.ui.components.TopBar
import org.vik.torrente.ui.components.TorrentList
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerType

@Composable
fun App(
    defaultDownloadDirectory: String,
    onOpenDirectory: (String) -> Unit,
    torrentService: TorrentService = remember(defaultDownloadDirectory) {
        MockTorrentService(defaultDownloadPath = defaultDownloadDirectory)
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

        val torrentFilePicker = rememberFilePickerLauncher(
            type = PickerType.File(extensions = listOf("torrent")),
            title = "Select .torrent file"
        ) { file ->
            if (file != null) {
                val path = file.path
                if (path != null) {
                    coroutineScope.launch {
                        val parsed = torrentService.parseTorrent(path, file.name)
                        pendingTorrent = parsed
                    }
                }
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopBar(
                    torrents = torrents,
                    onAddTorrentClick = {
                        torrentFilePicker.launch()
                    }
                )
            }
        ) { innerPadding ->
            TorrentList(
                torrents = torrents,
                onTogglePlayPause = { id -> torrentService.togglePlayPause(id) },
                onDelete = { id -> torrentService.deleteTorrent(id) },
                onOpenFolder = { path -> onOpenDirectory(path) },
                onAddTorrentClick = {
                    torrentFilePicker.launch()
                },
                modifier = Modifier.padding(innerPadding)
            )

            // Add Torrent Dialog (activated when a .torrent file is picked)
            pendingTorrent?.let { parsedInfo ->
                AddTorrentDialog(
                    parsedInfo = parsedInfo,
                    initialDestinationPath = defaultDownloadDirectory,
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
