package com.mycelium.myapplication.data.db

import androidx.room.*
import com.mycelium.myapplication.data.model.ChunkUploadQueue
import com.mycelium.myapplication.data.model.UploadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ChunkUploadQueueDao {
    @Query("SELECT * FROM chunk_upload_queue WHERE status = :status ORDER BY createdAt ASC")
    fun getChunksWithStatus(status: UploadStatus): Flow<List<ChunkUploadQueue>>

    @Query("SELECT * FROM chunk_upload_queue WHERE status = :status ORDER BY createdAt ASC LIMIT 1")
    suspend fun getNextChunkToUpload(status: UploadStatus = UploadStatus.PENDING): ChunkUploadQueue?

    @Query("SELECT * FROM chunk_upload_queue WHERE sessionId = :sessionId")
    suspend fun getChunksForSession(sessionId: String): List<ChunkUploadQueue>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunk(chunk: ChunkUploadQueue): Long

    @Update
    suspend fun updateChunk(chunk: ChunkUploadQueue)

    @Delete
    suspend fun deleteChunk(chunk: ChunkUploadQueue)

    @Query("SELECT * FROM chunk_upload_queue WHERE sessionId = :sessionId AND chunkIndex = :chunkIndex")
    suspend fun getChunkBySessionAndIndex(sessionId: String, chunkIndex: Int): ChunkUploadQueue?

    @Query("UPDATE chunk_upload_queue SET status = :newStatus WHERE id = :chunkId")
    suspend fun updateChunkStatus(chunkId: Long, newStatus: UploadStatus)

    @Query("UPDATE chunk_upload_queue SET retryCount = retryCount + 1, status = :newStatus WHERE id = :chunkId")
    suspend fun incrementRetryAndUpdateStatus(chunkId: Long, newStatus: UploadStatus = UploadStatus.PENDING)
}