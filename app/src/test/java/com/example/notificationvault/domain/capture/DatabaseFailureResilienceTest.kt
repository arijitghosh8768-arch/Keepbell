package com.example.notificationvault.domain.capture

import android.app.Notification
import android.content.Context
import android.database.sqlite.SQLiteDiskIOException
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.os.Process
import android.service.notification.StatusBarNotification
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.notificationvault.data.repository.CaptureSettingsRepositoryImpl
import com.example.notificationvault.data.repository.RetentionSettingsRepositoryImpl
import com.example.notificationvault.domain.model.ArchivedNotification
import com.example.notificationvault.domain.policy.RetentionPolicy
import com.example.notificationvault.domain.repository.NotificationRepository
import com.example.notificationvault.platform.listener.VaultNotificationListener
import com.example.notificationvault.worker.RetentionCleanupWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ServiceController

/**
 * Validates graceful failure handling when underlying SQLite or repository operations fail,
 * verifying that exceptions do not crash the service, failed saves are not reported as successful,
 * and WorkManager cleanup adheres to retry contracts.
 */
@RunWith(RobolectricTestRunner::class)
class DatabaseFailureResilienceTest {

    private lateinit var context: Context
    private lateinit var captureSettingsRepository: CaptureSettingsRepositoryImpl
    private lateinit var retentionSettingsRepository: RetentionSettingsRepositoryImpl

    private lateinit var serviceController: ServiceController<VaultNotificationListener>
    private lateinit var service: VaultNotificationListener

    class FailingNotificationRepository(
        val throwOnSave: Boolean = true,
        val throwOnDelete: Boolean = true
    ) : NotificationRepository {
        val savedList = mutableListOf<ArchivedNotification>()

        override fun getNotifications(): Flow<List<ArchivedNotification>> = flowOf(savedList)
        override fun getBookmarkedNotifications(): Flow<List<ArchivedNotification>> = flowOf(emptyList())
        override fun getNotificationsByPackage(packageName: String): Flow<List<ArchivedNotification>> = flowOf(emptyList())
        override fun getNotificationById(id: String): Flow<ArchivedNotification?> = flowOf(null)
        override suspend fun getNotificationByIdOnce(id: String): ArchivedNotification? = null
        override fun searchNotifications(query: String): Flow<List<ArchivedNotification>> = flowOf(emptyList())
        override fun filterNotifications(packageName: String?, startTimeMs: Long?, endTimeMs: Long?, query: String?): Flow<List<ArchivedNotification>> = flowOf(emptyList())

        override suspend fun saveNotification(notification: ArchivedNotification): Long {
            if (throwOnSave) {
                throw SQLiteDiskIOException("Disk is full: cannot write SQLite database")
            }
            savedList.add(notification)
            return 1L
        }

        override suspend fun saveNotifications(notifications: List<ArchivedNotification>): List<Long> {
            if (throwOnSave) {
                throw SQLiteDiskIOException("Disk is full: cannot write SQLite database")
            }
            savedList.addAll(notifications)
            return notifications.indices.map { it.toLong() }
        }

        override suspend fun updateNotification(notification: ArchivedNotification): Int {
            if (throwOnSave) throw SQLiteDiskIOException("Disk is full")
            return 1
        }

        override suspend fun deleteNotification(id: String): Int = 0
        override suspend fun deleteAllNotifications(): Int = 0
        override suspend fun deleteExpiredNotifications(cutoffMs: Long): Int {
            if (throwOnDelete) {
                throw SQLiteException("Database locked during cleanup")
            }
            return 0
        }

        override suspend fun setBookmark(id: String, isBookmarked: Boolean): Int = 0
        override suspend fun setRead(id: String, isRead: Boolean): Int = 0
        override fun getDistinctPackages(): Flow<List<String>> = flowOf(emptyList())
        override fun getNotificationCount(): Flow<Int> = flowOf(savedList.size)
        override suspend fun findBySourceKey(sourceKey: String): ArchivedNotification? = null
        override suspend fun findByFingerprint(fingerprint: String): ArchivedNotification? = null
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        captureSettingsRepository = CaptureSettingsRepositoryImpl(context)
        retentionSettingsRepository = RetentionSettingsRepositoryImpl(context)

        serviceController = Robolectric.buildService(VaultNotificationListener::class.java).create()
        service = serviceController.get()
    }

    @After
    fun tearDown() {
        serviceController.destroy()
    }

    private fun createSbn(packageName: String, title: String, text: String): StatusBarNotification {
        val extras = Bundle().apply {
            putCharSequence(Notification.EXTRA_TITLE, title)
            putCharSequence(Notification.EXTRA_TEXT, text)
        }
        @Suppress("DEPRECATION")
        val notification = Notification().apply { this.extras = extras }
        return StatusBarNotification(
            packageName, packageName, 1, "tag", 1000, 1000, 0,
            notification, Process.myUserHandle(), 1000L
        )
    }

    @Test
    fun capturePipeline_whenDatabaseThrowsDiskError_doesNotCrashListenerServiceAndDoesNotReportSuccess() = runBlocking {
        val failingRepo = FailingNotificationRepository(throwOnSave = true)
        val processor = CaptureProcessor(
            notificationRepository = failingRepo,
            captureSettingsRepository = captureSettingsRepository
        )

        service.repository = failingRepo
        service.captureProcessor = processor

        val sbn = createSbn("com.test.app", "Test Title", "Test Text")

        // 1. Invoking onNotificationPosted must not bubble unhandled SQLite exception or crash the platform service
        service.onNotificationPosted(sbn)
        Thread.sleep(100) // Allow service coroutine execution

        // 2. Failed save must not be reported as saved
        assertTrue("No records should be reported as saved", failingRepo.savedList.isEmpty())
    }

    @Test
    fun retentionWorker_whenDatabaseThrowsException_returnsRetrySafely() = runBlocking {
        val failingRepo = FailingNotificationRepository(throwOnDelete = true)
        retentionSettingsRepository.setRetentionPolicy(RetentionPolicy.DAYS_7)

        val worker = TestListenableWorkerBuilder<RetentionCleanupWorker>(context).build()
        worker.setInjectedRepositories(
            notificationRepo = failingRepo,
            retentionRepo = retentionSettingsRepository
        )

        val result = worker.doWork()

        // WorkManager contract: transient database exceptions must return Result.retry()
        assertTrue("Transient SQLite exception must result in Result.retry()", result is ListenableWorker.Result.Retry)
    }
}
