package com.mycelium.myapplication.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mycelium.myapplication.data.model.RecordingSession

@Database(
    entities = [RecordingSession::class],
    version = 1
)
@TypeConverters(Converters::class)
abstract class RecordingDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
}

@androidx.room.TypeConverters
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