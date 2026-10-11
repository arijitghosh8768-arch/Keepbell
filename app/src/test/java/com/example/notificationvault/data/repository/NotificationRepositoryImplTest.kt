package com.example.notificationvault.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.notificationvault.data.local.VaultDatabase
import com.example.notificationvault.domain.model.ArchivedNotification
import com.example.notificationvault.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class NotificationRepositoryImplTest {

    private lateinit var database: VaultDatabase
    private lateinit var repository: NotificationRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = VaultDatabase.createInMemoryDatabase(context)
        repository = NotificationRepositoryImpl(
            notificationDao = database.notificationDao(),
            ioDispatcher = testDispatcher
        )
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    @Test
    fun getNotifications_whenEmpty_returnsEmptyList() = runTest(testDispatcher) {
        val list = repository.getNotifications().first()
        assertTrue(list.isEmpty())
    }

    @Test
    fun saveNotification_and_getNotificationByIdOnce_returnsDomainModel() = runTest(testDispatcher) {
        val notification = ArchivedNotification(
            id = "repo-id-1",
            packageName = "com.chat",
            appLabel = "ChatApp",
            title = "Alice",
            body = "Hey!",
            eventTimeMs = 1000L,
            capturedAtMs = 1005L
        )

        repository.saveNotification(notification)
        val loaded = repository.getNotificationByIdOnce("repo-id-1")

        assertNotNull(loaded)
        assertEquals("repo-id-1", loaded?.id)
        assertEquals("ChatApp", loaded?.displayAppLabel)
        assertEquals("Hey!", loaded?.displayBody)
    }

    @Test
    fun observeNotificationById_emitsLiveUpdates() = runTest(testDispatcher) {
        val notification = ArchivedNotification(
            id = "repo-obs-1",
            packageName = "com.news",
            title = "Breaking",
            body = "Important news",
            eventTimeMs = 2000L,
            capturedAtMs = 2005L
        )
        repository.saveNotification(notification)

        val observed = repository.getNotificationById("repo-obs-1").first()
        assertNotNull(observed)
        assertEquals("Breaking", observed?.title)
    }

    @Test
    fun getBookmarkedNotifications_returnsOnlyBookmarkedItems() = runTest(testDispatcher) {
        val unbookmarked = ArchivedNotification(
            id = "id-1",
            packageName = "app1",
            eventTimeMs = 1L,
            capturedAtMs = 1L,
            isBookmarked = false
        )
        val bookmarked = ArchivedNotification(
            id = "id-2",
            packageName = "app2",
            eventTimeMs = 2L,
            capturedAtMs = 2L,
            isBookmarked = true
        )

        repository.saveNotifications(listOf(unbookmarked, bookmarked))

        val result = repository.getBookmarkedNotifications().first()
        assertEquals(1, result.size)
        assertEquals("id-2", result[0].id)
    }

    @Test
    fun getNotificationsByPackage_filtersCorrectly() = runTest(testDispatcher) {
        val pkg1 = ArchivedNotification(id = "1", packageName = "com.app.a", eventTimeMs = 1L, capturedAtMs = 1L)
        val pkg2 = ArchivedNotification(id = "2", packageName = "com.app.b", eventTimeMs = 2L, capturedAtMs = 2L)

        repository.saveNotifications(listOf(pkg1, pkg2))

        val results = repository.getNotificationsByPackage("com.app.a").first()
        assertEquals(1, results.size)
        assertEquals("com.app.a", results[0].packageName)
    }

    @Test
    fun searchNotifications_withEmptyQuery_returnsAll() = runTest(testDispatcher) {
        val item = ArchivedNotification(id = "1", packageName = "com.app", eventTimeMs = 1L, capturedAtMs = 1L)
        repository.saveNotification(item)

        val results = repository.searchNotifications("").first()
        assertEquals(1, results.size)
    }

    @Test
    fun searchNotifications_withQuery_matchesExpectedRecords() = runTest(testDispatcher) {
        val item1 = ArchivedNotification(
            id = "s-1",
            packageName = "com.bank",
            title = "OTP verification",
            body = "Your code is 1234",
            eventTimeMs = 1L,
            capturedAtMs = 1L
        )
        val item2 = ArchivedNotification(
            id = "s-2",
            packageName = "com.chat",
            title = "Bob",
            body = "Hello",
            eventTimeMs = 2L,
            capturedAtMs = 2L
        )

        repository.saveNotifications(listOf(item1, item2))

        val results = repository.searchNotifications("verification").first()
        assertEquals(1, results.size)
        assertEquals("s-1", results[0].id)
    }

    @Test
    fun setBookmark_and_setRead_updatesStateSuccessfully() = runTest(testDispatcher) {
        val item = ArchivedNotification(
            id = "toggle-item",
            packageName = "com.app",
            eventTimeMs = 1L,
            capturedAtMs = 1L
        )
        repository.saveNotification(item)

        repository.setBookmark("toggle-item", true)
        repository.setRead("toggle-item", true)

        val updated = repository.getNotificationByIdOnce("toggle-item")
        assertEquals(true, updated?.isBookmarked)
        assertEquals(true, updated?.isRead)
    }

    @Test
    fun deleteNotification_removesSingleRecord() = runTest(testDispatcher) {
        val item1 = ArchivedNotification(id = "d-1", packageName = "a", eventTimeMs = 1L, capturedAtMs = 1L)
        val item2 = ArchivedNotification(id = "d-2", packageName = "b", eventTimeMs = 2L, capturedAtMs = 2L)

        repository.saveNotifications(listOf(item1, item2))
        repository.deleteNotification("d-1")

        assertNull(repository.getNotificationByIdOnce("d-1"))
        assertNotNull(repository.getNotificationByIdOnce("d-2"))
    }

    @Test
    fun deleteAllNotifications_clearsAllRecords() = runTest(testDispatcher) {
        val item1 = ArchivedNotification(id = "d-1", packageName = "a", eventTimeMs = 1L, capturedAtMs = 1L)
        repository.saveNotification(item1)

        repository.deleteAllNotifications()
        val remaining = repository.getNotifications().first()
        assertTrue(remaining.isEmpty())
    }

    @Test
    fun deleteExpiredNotifications_protectsBookmarkedItems() = runTest(testDispatcher) {
        val cutoff = 1000L
        val expired = ArchivedNotification(id = "exp", packageName = "a", capturedAtMs = 500L, eventTimeMs = 500L, isBookmarked = false)
        val expiredBookmarked = ArchivedNotification(id = "bm", packageName = "b", capturedAtMs = 500L, eventTimeMs = 500L, isBookmarked = true)

        repository.saveNotifications(listOf(expired, expiredBookmarked))
        repository.deleteExpiredNotifications(cutoff)

        assertNull(repository.getNotificationByIdOnce("exp"))
        assertNotNull(repository.getNotificationByIdOnce("bm"))
    }

    @Test
    fun findBySourceKey_and_findByFingerprint_returnsCorrectRecords() = runTest(testDispatcher) {
        val item = ArchivedNotification(
            id = "key-test-id",
            packageName = "com.app",
            eventTimeMs = 10L,
            capturedAtMs = 10L,
            sourceNotificationKey = "source-key-999",
            dedupeFingerprint = "fingerprint-888"
        )
        repository.saveNotification(item)

        val foundKey = repository.findBySourceKey("source-key-999")
        assertEquals("key-test-id", foundKey?.id)

        val foundFp = repository.findByFingerprint("fingerprint-888")
        assertEquals("key-test-id", foundFp?.id)
    }

    @Test
    fun getNotificationCount_tracksInsertionsAndDeletions() = runTest(testDispatcher) {
        assertEquals(0, repository.getNotificationCount().first())

        repository.saveNotification(ArchivedNotification(id = "1", packageName = "a", eventTimeMs = 1L, capturedAtMs = 1L))
        assertEquals(1, repository.getNotificationCount().first())

        repository.deleteNotification("1")
        assertEquals(0, repository.getNotificationCount().first())
    }
}
