package com.mycelium.myapplication.data.db

import androidx.room.*
import com.mycelium.myapplication.data.model.RecordingResult
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingResultDao {
    @Query("SELECT * FROM recording_results WHERE sessionId = :recordingId ORDER BY timestamp DESC")
    fun getResultsForRecording(recordingId: String): Flow<List<RecordingResult>>
    
    @Query("SELECT * FROM recording_results WHERE sessionId = :recordingId AND resultType = :resultType LIMIT 1")
    suspend fun getResultByType(recordingId: String, resultType: String): RecordingResult?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: RecordingResult): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(results: List<RecordingResult>)
    
    @Update
    suspend fun updateResult(result: RecordingResult)
    
    @Delete
    suspend fun deleteResult(result: RecordingResult)
    
    @Query("DELETE FROM recording_results WHERE sessionId = :recordingId")
    suspend fun deleteResultsForRecording(recordingId: String)
}