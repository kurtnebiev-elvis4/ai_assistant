package com.mycelium.myapplication.data.repository

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class UploadChunkWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val recordingRepository: RecordingRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting work to retry failed uploads")
        return try {
            // Process pending uploads
            recordingRepository.startUploadQueue()

            // Retry failed uploads
            recordingRepository.retryFailedUploads()

            Log.d(TAG, "Successfully processed upload queue")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing upload queue", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "UploadChunkWorker"
        private const val WORK_NAME = "upload_chunk_worker"

        fun schedulePeriodicUploadWorker(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<UploadChunkWorker>(
                15, TimeUnit.MINUTES,  // Retry every 15 minutes
                5, TimeUnit.MINUTES    // With flexibility window of 5 minutes
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,  // Keep existing if already scheduled
                workRequest
            )
        }

        fun enqueueOneTimeUploadWorker(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<UploadChunkWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}