package com.mycelium.myapplication.ui.recording

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mycelium.myapplication.data.model.ChunkUploadQueue
import com.mycelium.myapplication.data.model.RecordingSession
import com.mycelium.myapplication.data.model.UploadStatus
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecordingList(
    recordings: List<RecordingSession>,
    onDeleteRecording: (RecordingSession) -> Unit,
    onPlayRecording: (RecordingSession) -> Unit,
    onShareRecording: (RecordingSession) -> Unit,
    onViewResults: (RecordingSession) -> Unit,
    onToggleChunksView: (RecordingSession) -> Unit,
    currentPlayingSession: String? = null,
    chunksMap: Map<String, List<ChunkUploadQueue>> = emptyMap()
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(recordings) { recording ->
            RecordingItem(
                recording = recording,
                onDelete = { onDeleteRecording(recording) },
                onPlay = { onPlayRecording(recording) },
                onShare = { onShareRecording(recording) },
                onViewResults = { onViewResults(recording) },
                onToggleChunksView = { onToggleChunksView(recording) },
                isPlaying = currentPlayingSession == recording.id,
                chunks = chunksMap[recording.id] ?: emptyList()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RecordingItemPreview() {
    RecordingItem(
        recording = RecordingSession(),
        onDelete = {},
        onPlay = {},
        onShare = {},
        onViewResults = {},
        onToggleChunksView = {},
        isPlaying = false,
        chunks = emptyList()
    )
}

@Composable
private fun RecordingItem(
    recording: RecordingSession,
    onDelete: () -> Unit,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onViewResults: () -> Unit,
    onToggleChunksView: () -> Unit,
    isPlaying: Boolean = false,
    chunks: List<ChunkUploadQueue> = emptyList()
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onViewResults
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top row with date and buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDate(recording.startTime),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = onPlay) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause recording" else "Play recording",
                        tint = if (isPlaying) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                }
                var menuExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share") },
                            onClick = {
                                menuExpanded = false
                                onShare()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Duration and file size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Duration: ${recording.formattedDuration}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Size: ${recording.formattedFileSize}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Audio file path
            Text(
                text = recording.audioFilePath ?: "No audio file",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(
                onClick = onToggleChunksView,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = if (recording.showChunks) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (recording.showChunks) "Hide chunks" else "Show chunks"
                )
            }

            // Chunks list
            if (recording.showChunks) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Text(
                    text = "Chunks (${chunks.size})",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                if (chunks.isEmpty()) {
                    Text(
                        text = "No chunks available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        chunks.forEach { chunk ->
                            ChunkStatusItem(chunk = chunk)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChunkStatusItem(chunk: ChunkUploadQueue) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Chunk #${chunk.chunkIndex + 1}${if (chunk.isLastChunk) " (Last)" else ""}",
                style = MaterialTheme.typography.bodyMedium
            )

            ChunkStatusBadge(status = chunk.status)
        }
    }
}

@Composable
private fun ChunkStatusBadge(status: UploadStatus) {
    val (color, text) = when (status) {
        UploadStatus.PENDING -> Pair(MaterialTheme.colorScheme.tertiary, "Pending")
        UploadStatus.IN_PROGRESS -> Pair(MaterialTheme.colorScheme.primary, "Uploading")
        UploadStatus.COMPLETED -> Pair(MaterialTheme.colorScheme.secondary, "Completed")
        UploadStatus.FAILED -> Pair(MaterialTheme.colorScheme.error, "Failed")
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}