package com.example.notificationvault.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.notificationvault.data.local.VaultDatabase
import com.example.notificationvault.data.repository.NotificationRepositoryImpl
import com.example.notificationvault.data.repository.RetentionSettingsRepositoryImpl
import com.example.notificationvault.domain.model.ArchivedNotification
import com.example.notificationvault.domain.policy.RetentionPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RetentionCleanupWorkerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var database: VaultDatabase
    private lateinit var notificationRepository: NotificationRepositoryImpl
    private lateinit var retentionSettingsRepository: RetentionSettingsRepositoryImpl

    private val fixedNowMs = 1_000_000_000_000L

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        database = VaultDatabase.createInMemoryDatabase(context)
        notificationRepository = NotificationRepositoryImpl(database.notificationDao(), testDispatcher)
        retentionSettingsRepository = RetentionSettingsRepositoryImpl(context, testDispatcher)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private suspend fun runCleanup(clock: () -> Long = { fixedNowMs }): Int {
        return RetentionCleanupWorker.executeCleanup(
            settingsRepository = retentionSettingsRepository,
            notificationRepository = notificationRepository,
            clock = clock
        )
    }

    @Test
    fun executeCleanup_boundaryCheck_deletesBeforeCutoff_preservesAtAndAfterCutoff() = runTest(testDispatcher) {
        // Setup 30-day retention
        retentionSettingsRepository.setRetentionPolicy(RetentionPolicy.DAYS_30)
        val cutoffMs = RetentionPolicy.DAYS_30.calculateCutoffMs(fixedNowMs)!!

        // Item 1: strictly before cutoff (cutoffMs - 1) -> must be deleted
        val itemExpired = ArchivedNotification(
            id = "expired_1",
            packageName = "com.test.app",
            title = "Expired",
            isBookmarked = false,
            eventTimeMs = cutoffMs - 1L,
            capturedAtMs = cutoffMs - 1L
        )
        // Item 2: exactly at cutoff (cutoffMs) -> must be retained
        val itemAtCutoff = ArchivedNotification(
            id = "boundary_at",
            packageName = "com.test.app",
            title = "At Cutoff",
            isBookmarked = false,
            eventTimeMs = cutoffMs,
            capturedAtMs = cutoffMs
        )
        // Item 3: strictly after cutoff (cutoffMs + 1) -> must be retained
        val itemAfterCutoff = ArchivedNotification(
            id = "boundary_after",
            packageName = "com.test.app",
            title = "After Cutoff",
            isBookmarked = false,
            eventTimeMs = cutoffMs + 1L,
            capturedAtMs = cutoffMs + 1L
        )

        notificationRepository.saveNotification(itemExpired)
        notificationRepository.saveNotification(itemAtCutoff)
        notificationRepository.saveNotification(itemAfterCutoff)

        val deletedCount = runCleanup()

        assertEquals(1, deletedCount)
        assertNull("Item before cutoff should be deleted", notificationRepository.getNotificationByIdOnce("expired_1"))
        assertNotNull("Item at cutoff boundary should be retained", notificationRepository.getNotificationByIdOnce("boundary_at"))
        assertNotNull("Item after cutoff should be retained", notificationRepository.getNotificationByIdOnce("boundary_after"))
    }

    @Test
    fun executeCleanup_preservesBookmarkedRecordsRegardlessOfAge() = runTest(testDispatcher) {
        // Setup 7-day retention
        retentionSettingsRepository.setRetentionPolicy(RetentionPolicy.DAYS_7)
        val cutoffMs = RetentionPolicy.DAYS_7.calculateCutoffMs(fixedNowMs)!!

        // Old bookmarked notification
        val bookmarkedOld = ArchivedNotification(
            id = "starred_old",
            packageName = "com.test.app",
            title = "Important Starred",
            isBookmarked = true,
            eventTimeMs = cutoffMs - 100_000L,
            capturedAtMs = cutoffMs - 100_000L
        )
        // Old unbookmarked notification
        val unbookmarkedOld = ArchivedNotification(
            id = "unstarred_old",
            packageName = "com.test.app",
            title = "Regular Old",
            isBookmarked = false,
            eventTimeMs = cutoffMs - 100_000L,
            capturedAtMs = cutoffMs - 100_000L
        )

        notificationRepository.saveNotification(bookmarkedOld)
        notificationRepository.saveNotification(unbookmarkedOld)

        val deletedCount = runCleanup()

        assertEquals(1, deletedCount)
        assertNotNull("Bookmarked record must be retained regardless of age", notificationRepository.getNotificationByIdOnce("starred_old"))
        assertNull("Unbookmarked expired record must be deleted", notificationRepository.getNotificationByIdOnce("unstarred_old"))
    }

    @Test
    fun executeCleanup_whenKeepUntilDeleted_doesNotDeleteAnyRecords() = runTest(testDispatcher) {
        retentionSettingsRepository.setRetentionPolicy(RetentionPolicy.KEEP_UNTIL_DELETED)

        val veryOld = ArchivedNotification(
            id = "very_old",
            packageName = "com.test.app",
            title = "Ancient Alert",
            isBookmarked = false,
            eventTimeMs = 1000L,
            capturedAtMs = 1000L
        )
        notificationRepository.saveNotification(veryOld)

        val deletedCount = runCleanup()

        assertEquals(0, deletedCount)
        assertNotNull("Ancient record should be preserved when Keep until deleted is set", notificationRepository.getNotificationByIdOnce("very_old"))
    }

    @Test
    fun executeCleanup_isIdempotent() = runTest(testDispatcher) {
        retentionSettingsRepository.setRetentionPolicy(RetentionPolicy.DAYS_30)
        val cutoffMs = RetentionPolicy.DAYS_30.calculateCutoffMs(fixedNowMs)!!

        val item1 = ArchivedNotification(id = "item_1", packageName = "com.a", eventTimeMs = cutoffMs - 10L, capturedAtMs = cutoffMs - 10L, isBookmarked = false)
        val item2 = ArchivedNotification(id = "item_2", packageName = "com.b", eventTimeMs = cutoffMs + 10L, capturedAtMs = cutoffMs + 10L, isBookmarked = false)
        notificationRepository.saveNotification(item1)
        notificationRepository.saveNotification(item2)

        // First run
        val deletedFirst = runCleanup()
        assertEquals(1, deletedFirst)
        assertNull(notificationRepository.getNotificationByIdOnce("item_1"))
        assertNotNull(notificationRepository.getNotificationByIdOnce("item_2"))

        // Second run immediately afterwards (idempotency check)
        val deletedSecond = runCleanup()
        assertEquals(0, deletedSecond)
        assertNull(notificationRepository.getNotificationByIdOnce("item_1"))
        assertNotNull(notificationRepository.getNotificationByIdOnce("item_2"))
    }
}
