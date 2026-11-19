package org.vik.torrente.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import org.vik.torrente.model.ParsedTorrentInfo
import org.vik.torrente.model.TorrentMode
import org.vik.torrente.ui.icons.AppIcons
import org.vik.torrente.ui.util.Formatters
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher

@Composable
fun AddTorrentDialog(
    parsedInfo: ParsedTorrentInfo,
    initialDestinationPath: String,
    onConfirm: (destinationPath: String, selectedFileIds: Set<String>, mode: TorrentMode) -> Unit,
    onDismiss: () -> Unit
) {
    var destinationPath by remember(initialDestinationPath) { mutableStateOf(initialDestinationPath) }
    var selectedFileIds by remember(parsedInfo) {
        mutableStateOf(parsedInfo.files.map { it.id }.toSet())
    }
    var selectedMode by remember { mutableStateOf(TorrentMode.DOWNLOAD) }
    
    val directoryPicker = rememberDirectoryPickerLauncher(
        title = "Select Download Destination Folder",
        initialDirectory = initialDestinationPath
    ) { file ->
        if (file != null) {
            val path = file.path
            if (path != null) {
                destinationPath = path
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        // Material 3 Dialog: 28.dp corner radius & surfaceContainerHigh
        Card(
            modifier = Modifier
                .width(620.dp)
                .heightIn(max = 640.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Text(
                    text = "Add Torrent",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = parsedInfo.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(18.dp))

                // --- Download Location Section ---
                Text(
                    text = "Download Location",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = destinationPath,
                        onValueChange = { destinationPath = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("Choose download location...") },
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

                    Button(
                        onClick = {
                            directoryPicker.launch()
                        }
                    ) {
                        Icon(
                            imageVector = AppIcons.FolderOpen,
                            contentDescription = "Browse folder",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Browse...")
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // --- Mode Selection Section ---
                Text(
                    text = "Mode",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = selectedMode == TorrentMode.DOWNLOAD,
                        onClick = { selectedMode = TorrentMode.DOWNLOAD },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) {
                        Text("Download")
                    }
                    SegmentedButton(
                        selected = selectedMode == TorrentMode.DOWNLOAD_AND_SEED,
                        onClick = { selectedMode = TorrentMode.DOWNLOAD_AND_SEED },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) {
                        Text("Download & Seed")
                    }
                    SegmentedButton(
                        selected = selectedMode == TorrentMode.SEED,
                        onClick = { selectedMode = TorrentMode.SEED },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) {
                        Text("Seed Only")
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // --- Torrent Files Section ---
                val totalSelectedBytes = parsedInfo.files
                    .filter { it.id in selectedFileIds }
                    .sumOf { it.sizeBytes }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Files (${selectedFileIds.size}/${parsedInfo.files.size} selected, ${Formatters.formatBytes(totalSelectedBytes)})",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = { selectedFileIds = parsedInfo.files.map { it.id }.toSet() }
                        ) {
                            Text("Select All", style = MaterialTheme.typography.labelMedium)
                        }
                        TextButton(
                            onClick = { selectedFileIds = emptySet() }
                        ) {
                            Text("Deselect All", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Scrollable File List in a surfaceContainer box
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(min = 120.dp, max = 240.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    LazyColumn {
                        items(parsedInfo.files, key = { it.id }) { file ->
                            val isChecked = file.id in selectedFileIds

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedFileIds = if (isChecked) {
                                            selectedFileIds - file.id
                                        } else {
                                            selectedFileIds + file.id
                                        }
                                    }
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedFileIds = if (checked) {
                                            selectedFileIds + file.id
                                        } else {
                                            selectedFileIds - file.id
                                        }
                                    }
                                )

                                Icon(
                                    imageVector = AppIcons.File,
                                    contentDescription = "File",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = file.path,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = Formatters.formatBytes(file.sizeBytes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(14.dp))

                // Dialog Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            onConfirm(destinationPath, selectedFileIds, selectedMode)
                        },
                        enabled = selectedFileIds.isNotEmpty() && destinationPath.isNotBlank()
                    ) {
                        Text("Add Torrent")
                    }
                }
            }
        }
    }
}
