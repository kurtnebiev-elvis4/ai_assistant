package com.mycelium.myapplication

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.mycelium.myapplication.data.repository.ServerManager
import com.mycelium.myapplication.data.repository.UploadChunkWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class TheApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var serverManager: ServerManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Ensure server manager is initialized and restore the selected server
        runBlocking {
            try {
                val server = serverManager.selectedServer.first()
                Log.d("TheApplication", "Restored selected server: ${server?.name ?: "None"}")
            } catch (e: Exception) {
                Log.e("TheApplication", "Error restoring selected server", e)
            }
        }

        UploadChunkWorker.enqueueOneTimeUploadWorker(this)
        // Schedule periodic worker to retry failed uploads
        UploadChunkWorker.schedulePeriodicUploadWorker(this)
    }
}