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
import org.vik.torrente.repository.models.TorrentModeModel
import org.vik.torrente.ui.model.ParsedTorrentInfo
import org.vik.torrente.ui.icons.AppIcons
import org.vik.torrente.ui.util.Formatters
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher

@Composable
fun AddTorrentDialog(
    parsedInfo: ParsedTorrentInfo,
    initialDestinationPath: String,
    onConfirm: (destinationPath: String, mode: TorrentModeModel) -> Unit,
    onDismiss: () -> Unit
) {
    var destinationPath by remember(initialDestinationPath) { mutableStateOf(initialDestinationPath) }
    var selectedMode by remember { mutableStateOf(TorrentModeModel.DOWNLOADING) }
    
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
                        selected = selectedMode == TorrentModeModel.DOWNLOADING,
                        onClick = { selectedMode = TorrentModeModel.DOWNLOADING },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) {
                        Text("Download")
                    }
                    SegmentedButton(
                        selected = selectedMode == TorrentModeModel.DOWNLOADING_AND_SEEDING,
                        onClick = { selectedMode = TorrentModeModel.DOWNLOADING_AND_SEEDING },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) {
                        Text("Download & Seed")
                    }
                    SegmentedButton(
                        selected = selectedMode == TorrentModeModel.SEEDING,
                        onClick = { selectedMode = TorrentModeModel.SEEDING },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) {
                        Text("Seed Only")
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // --- Mode Selection Section ---
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
                            onConfirm(destinationPath, selectedMode)
                        },
                        enabled = destinationPath.isNotBlank()
                    ) {
                        Text("Add Torrent")
                    }
                }
            }
        }
    }
}
