package com.mycelium.myapplication

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.mycelium.myapplication.data.repository.UploadChunkWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TheApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        UploadChunkWorker.enqueueOneTimeUploadWorker(this)
        // Schedule periodic worker to retry failed uploads
        UploadChunkWorker.schedulePeriodicUploadWorker(this)
    }
}