package org.vik.torrente.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import org.vik.torrente.ui.model.Torrent
import org.vik.torrente.repository.models.TorrentStatusModel
import org.vik.torrente.repository.models.TorrentModeModel
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import org.vik.torrente.ui.icons.AppIcons
import org.vik.torrente.ui.util.Formatters

@Composable
fun TorrentItemCard(
    torrent: Torrent,
    onTogglePlayPause: (String) -> Unit,
    onStop: (String) -> Unit,
    onDelete: (String) -> Unit,
    onOpenFolder: (String) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- LEFT SIDE: Action Buttons ---
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play/Pause button styled with Material You primary / container roles
                val isRunning = torrent.mode == TorrentModeModel.DOWNLOADING ||
                    torrent.mode == TorrentModeModel.DOWNLOADING_AND_SEEDING ||
                    torrent.mode == TorrentModeModel.SEEDING
                FilledIconButton(
                    onClick = { onTogglePlayPause(torrent.id) },
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isRunning) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        contentColor = if (isRunning) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimary
                        }
                    )
                ) {
                    Icon(
                        imageVector = if (isRunning) AppIcons.Pause else AppIcons.Play,
                        contentDescription = if (isRunning) "Pause" else "Resume",
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { onStop(torrent.id) },
                    modifier = Modifier.size(40.dp),
                    enabled = isRunning ||
                        torrent.mode == TorrentModeModel.PAUSED_DOWNLOADING ||
                        torrent.mode == TorrentModeModel.PAUSED_DOWNLOADING_AND_SEEDING ||
                        torrent.mode == TorrentModeModel.PAUSED_SEEDING,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.outline
                    )
                ) {
                    Icon(
                        imageVector = AppIcons.Stop,
                        contentDescription = "Stop torrent",
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Folder button always visible
                FilledIconButton(
                    onClick = { onOpenFolder(torrent.destinationPath) },
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Icon(
                        imageVector = AppIcons.Folder,
                        contentDescription = "Open download folder",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Delete button
                IconButton(
                    onClick = { onDelete(torrent.id) },
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = AppIcons.Delete,
                        contentDescription = "Delete torrent",
                        modifier = Modifier.size(20.dp)
                    )
                }

            }

            Spacer(modifier = Modifier.width(16.dp))

            // --- MAIN BODY: Info, Progress Bar, Speeds ---
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Top Row: Name, Progress in Percent, Speed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Torrent's Name
                    Text(
                        text = torrent.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Progress in Percent and Speed
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Percent
                        Text(
                            text = torrent.progressPercent,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = when {
                                torrent.isFinished -> MaterialTheme.colorScheme.tertiary
                                torrent.status == TorrentStatusModel.PAUSED -> MaterialTheme.colorScheme.outline
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )

                        // Speed or Status
                        val speedLabel = when {
                            torrent.status == TorrentStatusModel.SEEDING && torrent.uploadSpeedBytesPerSec > 0 ->
                                "Seeding (↑ ${Formatters.formatSpeed(torrent.uploadSpeedBytesPerSec)})"
                            torrent.status == TorrentStatusModel.SEEDING -> "Seeding"
                            torrent.status == TorrentStatusModel.CHECKING -> "Checking"
                            torrent.status == TorrentStatusModel.STARTING -> "Starting"
                            torrent.status == TorrentStatusModel.STOPPED -> "Stopped"
                            torrent.status == TorrentStatusModel.ERROR -> "Error"
                            torrent.isFinished -> "Finished"
                            torrent.status == TorrentStatusModel.PAUSED -> "Paused"
                            torrent.downloadSpeedBytesPerSec > 0 ->
                                "↓ ${Formatters.formatSpeed(torrent.downloadSpeedBytesPerSec)}"
                            else -> ""
                        }

                        Text(
                            text = speedLabel,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            ),
                            color = when {
                                torrent.isFinished -> MaterialTheme.colorScheme.tertiary
                                torrent.status == TorrentStatusModel.PAUSED -> MaterialTheme.colorScheme.outline
                                else -> MaterialTheme.colorScheme.secondary
                            }
                        )

                        val bytesRemaining = torrent.totalBytes - torrent.downloadedBytes
                        //time is in seconds
                        val timeRemaining = if(torrent.downloadSpeedBytesPerSec != 0L){
                            bytesRemaining / torrent.downloadSpeedBytesPerSec
                        }else{
                            0
                        }
                        val remainingSeconds = timeRemaining % 60
                        val remainingMinutes = timeRemaining / 60
                        val formattedTime = if(!torrent.isFinished){
                            if(timeRemaining == 0L){
                                ""
                            }
                            else {
                                "${remainingMinutes} min ${remainingSeconds} sec remaining"
                            }
                        }else{
                            ""
                        }

                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            ),
                            color = when {
                                torrent.isFinished -> MaterialTheme.colorScheme.tertiary
                                torrent.status == TorrentStatusModel.PAUSED -> MaterialTheme.colorScheme.outline
                                else -> MaterialTheme.colorScheme.secondary
                            }
                        )
                    }
                }

                // Below that: Progress Bar (styled with Material You tokens)
                LinearProgressIndicator(
                    progress = { torrent.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = when {
                        torrent.isFinished -> MaterialTheme.colorScheme.tertiary
                        torrent.status == TorrentStatusModel.PAUSED -> MaterialTheme.colorScheme.outline
                        else -> MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )

                // Secondary metadata row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val downloadedStr = Formatters.formatBytes(torrent.downloadedBytes)
                    val totalStr = Formatters.formatBytes(torrent.totalBytes)
                    
                    val metadataStr = if (
                        torrent.status == TorrentStatusModel.STOPPED ||
                        torrent.status == TorrentStatusModel.PAUSED
                    ) {
                        "$downloadedStr of $totalStr"
                    } else {
                        "$downloadedStr of $totalStr • Peers: ${torrent.peers}"
                    }

                    Text(
                        text = metadataStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (torrent.destinationPath.isNotBlank()) {
                        Text(
                            text = "Save to: ${torrent.destinationPath}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
