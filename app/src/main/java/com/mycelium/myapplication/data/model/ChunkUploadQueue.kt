package com.mycelium.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chunk_upload_queue")
data class ChunkUploadQueue(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val chunkIndex: Int,
    val isLastChunk: Boolean,
    val filePath: String,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val status: UploadStatus = UploadStatus.PENDING
)

enum class UploadStatus {
    PENDING,
    IN_PROGRESS,
    FAILED,
    COMPLETED
}