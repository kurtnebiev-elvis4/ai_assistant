package com.mycelium.myapplication.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mycelium.myapplication.data.model.ChunkUploadQueue
import com.mycelium.myapplication.data.model.RecordingResult
import com.mycelium.myapplication.data.model.RecordingSession

@Database(
    entities = [RecordingSession::class, ChunkUploadQueue::class, RecordingResult::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3)
    ]
)
@TypeConverters(Converters::class)
abstract class RecordingDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
    abstract fun chunkUploadQueueDao(): ChunkUploadQueueDao
    abstract fun recordingResultDao(): RecordingResultDao
}

@TypeConverters
class Converters {
    @androidx.room.TypeConverter
    fun fromTimestamp(value: Long?): java.util.Date? {
        return value?.let { java.util.Date(it) }
    }

    @androidx.room.TypeConverter
    fun dateToTimestamp(date: java.util.Date?): Long? {
        return date?.time
    }
    
} 