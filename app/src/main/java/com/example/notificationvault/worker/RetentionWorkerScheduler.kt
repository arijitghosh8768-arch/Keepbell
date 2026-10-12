package com.example.notificationvault.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Helper to schedule or cancel unique periodic retention cleanup via WorkManager.
 */
object RetentionWorkerScheduler {

    /**
     * Schedules daily retention cleanup as unique periodic work.
     *
     * Using [ExistingPeriodicWorkPolicy.KEEP] ensures that an already scheduled periodic work
     * continues running without resetting its interval.
     */
    fun schedulePeriodicCleanup(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<RetentionCleanupWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            RetentionCleanupWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Cancels the scheduled retention cleanup work if needed.
     */
    fun cancelPeriodicCleanup(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(RetentionCleanupWorker.UNIQUE_WORK_NAME)
    }
}
