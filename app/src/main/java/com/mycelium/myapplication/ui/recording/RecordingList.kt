package com.mycelium.myapplication.ui.recording

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mycelium.myapplication.data.model.RecordingSession
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecordingList(
    recordings: List<RecordingSession>,
    onDeleteRecording: (RecordingSession) -> Unit,
    onPlayRecording: (RecordingSession) -> Unit,
    onViewResults: (RecordingSession) -> Unit
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
                onViewResults = { onViewResults(recording) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RecordingItemPreview() {
    RecordingItem(RecordingSession(), {}, {}, {})
}

@Composable
private fun RecordingItem(
    recording: RecordingSession,
    onDelete: () -> Unit,
    onPlay: () -> Unit,
    onViewResults: () -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDate(recording.startTime),
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onPlay) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play recording"
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete recording"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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

            Text(
                text = recording.audioFilePath ?: "No audio file",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
} 