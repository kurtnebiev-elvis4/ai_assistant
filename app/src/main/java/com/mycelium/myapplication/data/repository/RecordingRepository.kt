package com.mycelium.myapplication.data.repository

import android.content.Context
import androidx.core.net.toUri
import com.mycelium.myapplication.data.db.RecordingDao
import com.mycelium.myapplication.data.model.RecordingSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class RecordingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordingDao: RecordingDao,
    private val assistantApi: AssistantApi
) {
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

    suspend fun health() {
        assistantApi.checkHealth()
    }

    suspend fun uploadChunk(sessionId: String, chunkIndex: Int, isLastChunk: Boolean, file: File) {
        val mediaType = context.contentResolver.getType(file.toUri())
        val requestFile = RequestBody.create(mediaType?.toMediaTypeOrNull(), file)
        val body = MultipartBody.Part.createFormData("chunk", file.getName(), requestFile)
        assistantApi.uploadChunk(sessionId, chunkIndex, isLastChunk, body)
    }
}