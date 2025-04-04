package com.mycelium.myapplication.di

import android.content.Context
import androidx.room.Room
import com.mycelium.myapplication.data.db.RecordingDatabase
import com.mycelium.myapplication.data.db.RecordingDao
import com.mycelium.myapplication.data.repository.AssistantApi
import com.mycelium.myapplication.data.repository.RecordingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): RecordingDatabase {
        return Room.databaseBuilder(
            context,
            RecordingDatabase::class.java,
            "recording_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideRecordingDao(database: RecordingDatabase): RecordingDao {
        return database.recordingDao()
    }

    @Provides
    @Singleton
    fun provideRecordingRepository(
        @ApplicationContext context: Context,
        recordingDao: RecordingDao,
        assistantApi: AssistantApi
    ): RecordingRepository {
        return RecordingRepository(context, recordingDao, assistantApi)
    }
} 