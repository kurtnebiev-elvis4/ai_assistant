package com.mycelium.myapplication.di

import com.mycelium.myapplication.ui.recording.RecordListState
import com.mycelium.myapplication.ui.recording.RecordingState
import com.mycelium.myapplication.ui.recording.ResultUiState
import com.mycelium.myapplication.ui.recording.ServerUiState
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object UIModule {
    @Provides
    fun provideRecordingState(): RecordingState = RecordingState()

    @Provides
    fun provideResultState(): ResultUiState = ResultUiState()

    @Provides
    fun provideRecordListState(): RecordListState = RecordListState()

    @Provides
    fun provideServerState(): ServerUiState = ServerUiState()
}