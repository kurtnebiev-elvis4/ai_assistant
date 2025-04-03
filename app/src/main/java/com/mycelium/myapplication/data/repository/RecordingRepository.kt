package com.mycelium.myapplication.data.repository

import com.mycelium.myapplication.data.db.RecordingDao
import com.mycelium.myapplication.data.model.RecordingSession
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingRepository @Inject constructor(
    private val recordingDao: RecordingDao
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
} 