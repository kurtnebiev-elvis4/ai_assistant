package com.mycelium.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore
import java.io.File
import java.util.UUID
import com.mycelium.myapplication.data.model.ChunkUploadQueue

@Entity(tableName = "recording_sessions")
data class RecordingSession(
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),
    var startTime: Long = 0,
    var endTime: Long? = null,
    var audioFilePath: String? = null,
    var isUploaded: Boolean = false,
    var fileSize: Long = 0,
    // These fields are not persisted in the database - transient fields for UI state
    @Ignore
    var showChunks: Boolean = false,
    @Ignore
    var chunks: List<ChunkUploadQueue> = emptyList()
) {
    val duration: Long
        get() = if (chunks.isNotEmpty()) {
            // Calculate total duration from all chunks
            chunks.sumOf { chunk ->
                val chunkFile = File(chunk.filePath)
                if (chunkFile.exists()) {
                    // For WAV files: (file size - 44 header bytes) / (sample rate * channels * bits per sample / 8)
                    // Assuming 16kHz, mono, 16-bit audio (2 bytes per sample)
                    val audioDataSize = chunkFile.length() - 44 // Subtract WAV header size
                    val bytesPerSecond = 16000 * 1 * 2 // 16kHz * 1 channel * 2 bytes per sample
                    (audioDataSize * 1000 / bytesPerSecond) // Convert to milliseconds
                } else {
                    0L
                }
            }
        } else {
            // Fall back to session timestamps
            endTime?.let { it - startTime } ?: 0L
        }

    val formattedDuration: String
        get() = formatDuration(duration)

    val formattedFileSize: String
        get() = if (chunks.isNotEmpty()) {
            // Sum file sizes from all chunks
            val totalSize = chunks.sumOf { chunk ->
                File(chunk.filePath).let { if (it.exists()) it.length() else 0L }
            }
            formatFileSize(totalSize)
        } else {
            formatFileSize(fileSize)
        }

    companion object {
        private fun formatDuration(durationMs: Long): String {
            val seconds = durationMs / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            return when {
                hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
                minutes > 0 -> String.format("%02d:%02d", minutes, seconds % 60)
                else -> String.format("%02d", seconds)
            }
        }

        private fun formatFileSize(size: Long): String {
            val units = arrayOf("B", "KB", "MB", "GB")
            var fileSize = size.toDouble()
            var unitIndex = 0
            while (fileSize >= 1024 && unitIndex < units.size - 1) {
                fileSize /= 1024
                unitIndex++
            }
            return "%.1f %s".format(fileSize, units[unitIndex])
        }
    }
} 