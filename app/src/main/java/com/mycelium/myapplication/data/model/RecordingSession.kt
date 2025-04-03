package com.mycelium.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.File
import java.util.UUID

@Entity(tableName = "recording_sessions")
data class RecordingSession(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val startTime: Long = System.currentTimeMillis(),
    var endTime: Long? = null,
    var audioFilePath: String? = null,
    var isUploaded: Boolean = false,
    var fileSize: Long = 0
) {
    val duration: Long
        get() = endTime?.let { it - startTime } ?: 0L

    val formattedDuration: String
        get() = formatDuration(duration)

    val formattedFileSize: String
        get() = formatFileSize(fileSize)

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