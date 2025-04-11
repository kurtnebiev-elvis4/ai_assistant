package com.mycelium.myapplication.data.repository

import android.content.Context
import androidx.core.net.toUri
import com.mycelium.myapplication.data.db.ChunkUploadQueueDao
import com.mycelium.myapplication.data.db.RecordingDao
import com.mycelium.myapplication.data.db.RecordingResultDao
import com.mycelium.myapplication.data.model.ChunkUploadQueue
import com.mycelium.myapplication.data.model.RecordingResult
import com.mycelium.myapplication.data.model.RecordingSession
import com.mycelium.myapplication.data.model.UploadStatus
import com.mycelium.myapplication.data.recording.Chunk
import com.mycelium.myapplication.data.recording.getFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.create
import retrofit2.HttpException
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

val TASKS = listOf("transcript", "summary", "tasks", "decisions")

@Singleton
class RecordingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordingDao: RecordingDao,
    private val chunkUploadQueueDao: ChunkUploadQueueDao,
    private val recordingResultDao: RecordingResultDao,
    private val assistantApi: AssistantApi,
    private val serverManager: ServerManager
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun getAllRecordings(): Flow<List<RecordingSession>> = recordingDao.getAllRecordings()

    suspend fun insertRecording(recording: RecordingSession) {
        recordingDao.insertRecording(recording)
    }

    suspend fun updateRecording(recording: RecordingSession) {
        recordingDao.updateRecording(recording)
    }

    suspend fun deleteRecording(recording: RecordingSession) {
        recordingDao.deleteRecording(recording)
    }

    suspend fun getRecordingById(sessionId: String): RecordingSession? {
        return recordingDao.getRecordingById(sessionId)
    }

    suspend fun getChunksForSession(sessionId: String): List<ChunkUploadQueue> =
        chunkUploadQueueDao.getChunksForSession(sessionId)

    suspend fun health() {
        assistantApi.checkHealth()
    }

    suspend fun finishSession(sessionId: String) {
        assistantApi.sessionFinished(sessionId)
    }

    suspend fun getProcessingStatus(fileId: String): Map<String, Boolean> =
        assistantApi.getStatus(fileId)

    fun downloadResult(
        sessionId: String,
        resultType: List<String>,
        local: Boolean = false
    ): Flow<Pair<String, String>> =
        flow {
            val newResults = mutableListOf<RecordingResult>()

            // Check which result types are already in the database
            val cachedResults = mutableMapOf<String, String>()
            resultType.forEach { type ->
                val result = recordingResultDao.getResultByType(sessionId, type)
                if (result != null) {
                    cachedResults[type] = result.content
                    emit(type to result.content)
                }
            }
            if (local == true) {
                return@flow
            }

            // Identify result types that need to be fetched from the API
            val missingTypes = resultType.filter { !cachedResults.containsKey(it) }

            if (missingTypes.isEmpty()) {
                return@flow // All results were found in the database
            }

            // Download missing result types from the API
            missingTypes.forEach { type ->
                try {
                    val response = assistantApi.downloadResult(sessionId, type)
                    val content = response.body()?.string() ?: "No content available"

                    // Save the new result to the database
                    val recordingResult = RecordingResult(
                        sessionId = sessionId,
                        resultType = type,
                        content = content
                    )
                    newResults.add(recordingResult)

                    // Emit the result to the flow
                    emit(type to content)
                } catch (e: Exception) {
                    emit(type to "Error retrieving result: ${e.message}")
                }
            }

            // Save all new results to the database
            if (newResults.isNotEmpty()) {
                recordingResultDao.insertResults(newResults)
            }
        }

    fun getResultsForRecording(recordingId: String): Flow<List<RecordingResult>> {
        return recordingResultDao.getResultsForRecording(recordingId)
    }

    suspend fun getResultByType(recordingId: String, resultType: String): RecordingResult? {
        return recordingResultDao.getResultByType(recordingId, resultType)
    }

    suspend fun uploadChunk(chunk: Chunk, isLastChunk: Boolean) {
        // Add chunk to upload queue first
        val queueItem = ChunkUploadQueue(
            sessionId = chunk.sessionId,
            chunkIndex = chunk.index,
            isLastChunk = isLastChunk,
            filePath = chunk.getFile(context).absolutePath,
            status = UploadStatus.PENDING
        )

        val queueId = chunkUploadQueueDao.insertChunk(queueItem)

        // Try to upload immediately
        processChunkUpload(queueId)
    }
//    init {
//        GlobalScope.launch(Dispatchers.IO) {
//            val list = chunkUploadQueueDao.getChunksByStatus(UploadStatus.COMPLETED)
//            list.forEach {
//                chunkUploadQueueDao.updateChunk(it.copy(status = UploadStatus.PENDING))
//            }
//        }
//    }

    private suspend fun processChunkUpload(chunkId: Long) {
        withContext(Dispatchers.IO) {
            try {
                // Get the chunk from the queue
                val chunk = chunkUploadQueueDao.getChunksWithStatus(UploadStatus.PENDING).firstOrNull()
                    ?.firstOrNull { it.id == chunkId } ?: return@withContext
                if (chunk.isLastChunk) {
                    val incompleteChunks = chunkUploadQueueDao.getChunksForSession(chunk.sessionId)
                        .filter { it.chunkIndex < chunk.chunkIndex && it.status != UploadStatus.COMPLETED }

                    if (incompleteChunks.isNotEmpty()) {
                        // Previous chunks are not all uploaded yet, skip for now
                        return@withContext
                    }
                }

                // Mark as in progress
                chunkUploadQueueDao.updateChunkStatus(chunkId, UploadStatus.IN_PROGRESS)

                // Prepare file for upload
                val file = File(chunk.filePath)
                if (!file.exists()) {
                    // File no longer exists, remove from queue
                    chunkUploadQueueDao.deleteChunk(chunk)
                    return@withContext
                }

                val mediaType = context.contentResolver.getType(file.toUri())
                val requestFile = create(mediaType?.toMediaTypeOrNull(), file)
                val body = MultipartBody.Part.createFormData("chunk", file.name, requestFile)

                // Upload the chunk
                val response = assistantApi.uploadChunk(
                    chunk.sessionId,
                    chunk.chunkIndex,
                    chunk.isLastChunk,
                    body
                )

                if (response.isSuccessful) {
                    // Upload successful, remove from queue
                    chunkUploadQueueDao.updateChunkStatus(chunkId, UploadStatus.COMPLETED)
                    // Eventually we might want to clean up completed uploads after some time
                } else {
                    // Upload failed, mark for retry
                    chunkUploadQueueDao.incrementRetryAndUpdateStatus(chunkId, UploadStatus.FAILED)
                }
            } catch (e: HttpException) {
                // Network error, mark for retry
                chunkUploadQueueDao.incrementRetryAndUpdateStatus(chunkId, UploadStatus.FAILED)
            } catch (e: Exception) {
                // Other error, mark as failed
                chunkUploadQueueDao.incrementRetryAndUpdateStatus(chunkId, UploadStatus.FAILED)
            }
        }
    }

    // This method can be called from a worker or service to retry failed uploads
    suspend fun retryFailedUploads(maxRetries: Int = 10000) {
        withContext(Dispatchers.IO) {
            val failedChunks = chunkUploadQueueDao.getChunksWithStatus(UploadStatus.FAILED).firstOrNull() ?: emptyList()

            for (chunk in failedChunks) {
                if (chunk.retryCount < maxRetries) {
                    // Mark as pending and retry
                    chunkUploadQueueDao.updateChunkStatus(chunk.id, UploadStatus.PENDING)
                    processChunkUpload(chunk.id)
                }
            }
        }
    }

    // Retry uploading a specific chunk
    suspend fun retryChunkUpload(chunkId: Long) {
        withContext(Dispatchers.IO) {
            // Mark as pending and retry
            chunkUploadQueueDao.updateChunkStatus(chunkId, UploadStatus.PENDING)
            processChunkUpload(chunkId)
        }
    }

    // Retry all failed uploads for a specific session
    suspend fun retryUpload(chunks: List<Long>) {
        withContext(Dispatchers.IO) {
            chunks.forEach { chunkId ->
                chunkUploadQueueDao.updateChunkStatus(chunkId, UploadStatus.PENDING)
            }
            chunks.forEach { chunkId ->
                processChunkUpload(chunkId)
            }
        }
    }

    // Start a background process to check for pending uploads
    fun startUploadQueue() {
        coroutineScope.launch {
            val pendingChunks =
                chunkUploadQueueDao.getChunksWithStatus(UploadStatus.PENDING).firstOrNull() ?: emptyList()
            for (chunk in pendingChunks) {
                processChunkUpload(chunk.id)
            }
        }
    }
}