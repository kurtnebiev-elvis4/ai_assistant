package com.mycelium.myapplication

import android.app.Application
import android.util.Log
import com.mycelium.myapplication.data.repository.RecordingRepository
import com.mycelium.myapplication.data.repository.UploadChunkWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class TheApplication : Application() {
    @Inject
    lateinit var recordingRepository: RecordingRepository
    
    private val applicationScope = CoroutineScope(Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize upload queue on app start
        applicationScope.launch {
            try {
                // Start processing any pending uploads
                recordingRepository.startUploadQueue()
                // Attempt to retry any failed uploads
                recordingRepository.retryFailedUploads()
            } catch (e: Exception) {
                Log.e("TheApplication", "Error starting upload queue", e)
            }
        }
        
        // Schedule periodic worker to retry failed uploads
        UploadChunkWorker.schedulePeriodicUploadWorker(this)
    }
}