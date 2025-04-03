package com.mycelium.myapplication.data.db

import androidx.room.*
import com.mycelium.myapplication.data.model.RecordingSession
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recording_sessions ORDER BY startTime DESC")
    fun getAllRecordings(): Flow<List<RecordingSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: RecordingSession)

    @Update
    suspend fun updateRecording(recording: RecordingSession)

    @Delete
    suspend fun deleteRecording(recording: RecordingSession)

    @Query("SELECT * FROM recording_sessions WHERE id = :sessionId")
    suspend fun getRecordingById(sessionId: String): RecordingSession?
} 