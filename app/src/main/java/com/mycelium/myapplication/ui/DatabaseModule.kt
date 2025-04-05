package com.mycelium.myapplication.ui

import com.mycelium.myapplication.ui.recording.RecordingState
import com.mycelium.myapplication.ui.recording.ResultUiState
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
}