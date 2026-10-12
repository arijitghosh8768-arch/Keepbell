package com.example.notificationvault.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.notificationvault.data.local.VaultDatabase
import com.example.notificationvault.data.repository.NotificationRepositoryImpl
import com.example.notificationvault.data.repository.RetentionSettingsRepositoryImpl
import com.example.notificationvault.domain.policy.RetentionPolicy
import com.example.notificationvault.domain.policy.RetentionSettingsRepository
import com.example.notificationvault.domain.repository.NotificationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background WorkManager worker responsible for executing deterministic retention cleanup.
 *
 * Safeguards:
 * - Reads the current retention setting on every execution so user changes take effect immediately.
 * - If retention is [RetentionPolicy.KEEP_UNTIL_DELETED], no deletion occurs.
 * - Only records with capturedAtMs strictly less than the cutoff (< cutoffMs) and isBookmarked = 0
 *   are deleted. Bookmarks are strictly preserved.
 * - Idempotent execution: safe to run multiple times without unintended side-effects.
 * - Safely handles repository/preference failures without deleting records.
 */
class RetentionCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private var injectedNotificationRepository: NotificationRepository? = null
    private var injectedRetentionSettingsRepository: RetentionSettingsRepository? = null
    private var injectedDispatcher: CoroutineDispatcher = Dispatchers.IO
    private var injectedClock: () -> Long = { System.currentTimeMillis() }

    constructor(
        appContext: Context,
        workerParams: WorkerParameters,
        notificationRepository: NotificationRepository? = null,
        retentionSettingsRepository: RetentionSettingsRepository? = null,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        clock: () -> Long = { System.currentTimeMillis() }
    ) : this(appContext, workerParams) {
        this.injectedNotificationRepository = notificationRepository
        this.injectedRetentionSettingsRepository = retentionSettingsRepository
        this.injectedDispatcher = ioDispatcher
        this.injectedClock = clock
    }

    internal fun setInjectedRepositories(
        notificationRepo: NotificationRepository,
        retentionRepo: RetentionSettingsRepository
    ) {
        this.injectedNotificationRepository = notificationRepo
        this.injectedRetentionSettingsRepository = retentionRepo
    }

    override suspend fun doWork(): Result = withContext(injectedDispatcher) {
        try {
            val settingsRepo = injectedRetentionSettingsRepository ?: RetentionSettingsRepositoryImpl(applicationContext, injectedDispatcher)
            val notifRepo = injectedNotificationRepository ?: run {
                val db = VaultDatabase.getInstance(applicationContext)
                NotificationRepositoryImpl(db.notificationDao(), injectedDispatcher)
            }

            executeCleanup(
                settingsRepository = settingsRepo,
                notificationRepository = notifRepo,
                clock = injectedClock
            )

            Result.success()
        } catch (e: Exception) {
            // Transient or database failure: retry execution without performing partial or risky deletion
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "retention_cleanup_periodic_work"
        private const val MAX_RETRIES = 3

        suspend fun executeCleanup(
            settingsRepository: RetentionSettingsRepository,
            notificationRepository: NotificationRepository,
            clock: () -> Long = { System.currentTimeMillis() }
        ): Int {
            val policy = settingsRepository.getRetentionPolicy()
            val nowMs = clock()
            val cutoffMs = policy.calculateCutoffMs(nowMs) ?: return 0
            return notificationRepository.deleteExpiredNotifications(cutoffMs)
        }
    }
}
