package com.example.notificationvault.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class NotificationDaoTest {

    private lateinit var database: VaultDatabase
    private lateinit var dao: NotificationDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = VaultDatabase.createInMemoryDatabase(context)
        dao = database.notificationDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeAll_whenEmpty_returnsEmptyList() = runTest {
        val notifications = dao.observeAll().first()
        assertTrue(notifications.isEmpty())
    }

    @Test
    fun insertAndGetById_persistsNotificationCorrectly() = runTest {
        val notification = NotificationEntity(
            id = "test-uuid-1",
            packageName = "org.telegram.messenger",
            appLabel = "Telegram",
            title = "Alice",
            body = "Meeting at 3 PM",
            eventTimeMs = 1000L,
            capturedAtMs = 1005L,
            isBookmarked = false,
            isRead = false
        )

        dao.insert(notification)
        val loaded = dao.getById("test-uuid-1")

        assertNotNull(loaded)
        assertEquals("test-uuid-1", loaded?.id)
        assertEquals("org.telegram.messenger", loaded?.packageName)
        assertEquals("Telegram", loaded?.appLabel)
        assertEquals("Alice", loaded?.title)
        assertEquals("Meeting at 3 PM", loaded?.body)
        assertEquals(1000L, loaded?.eventTimeMs)
        assertEquals(1005L, loaded?.capturedAtMs)
        assertFalse(loaded?.isBookmarked ?: true)
        assertFalse(loaded?.isRead ?: true)
    }

    @Test
    fun insertWithNullBody_preservesRecordWithoutCrash() = runTest {
        val notification = NotificationEntity(
            id = "test-uuid-null-body",
            packageName = "com.google.android.gm",
            appLabel = "Gmail",
            title = "New Mail",
            body = null,
            eventTimeMs = 2000L,
            capturedAtMs = 2001L
        )

        dao.insert(notification)
        val loaded = dao.getById("test-uuid-null-body")

        assertNotNull(loaded)
        assertNull(loaded?.body)
    }

    @Test
    fun twoIdenticalAlerts_withDistinctIds_areBothPreserved() = runTest {
        val alert1 = NotificationEntity(
            id = "alert-1",
            packageName = "com.banking.app",
            appLabel = "Bank Alert",
            title = "Security Alert",
            body = "Login detected",
            eventTimeMs = 1000L,
            capturedAtMs = 1001L
        )
        val alert2 = NotificationEntity(
            id = "alert-2",
            packageName = "com.banking.app",
            appLabel = "Bank Alert",
            title = "Security Alert",
            body = "Login detected",
            eventTimeMs = 2000L,
            capturedAtMs = 2001L
        )

        dao.insert(alert1)
        dao.insert(alert2)

        val all = dao.observeAll().first()
        assertEquals(2, all.size)
    }

    @Test
    fun update_modifiesFields() = runTest {
        val original = NotificationEntity(
            id = "update-test",
            packageName = "com.whatsapp",
            title = "Bob",
            body = "Hello",
            eventTimeMs = 1000L,
            capturedAtMs = 1002L
        )
        dao.insert(original)

        val modified = original.copy(body = "Hello world!", isRead = true)
        dao.update(modified)

        val loaded = dao.getById("update-test")
        assertEquals("Hello world!", loaded?.body)
        assertTrue(loaded?.isRead ?: false)
    }

    @Test
    fun setBookmark_updatesOnlyBookmarkField() = runTest {
        val notification = NotificationEntity(
            id = "bm-test",
            packageName = "com.slack",
            title = "Standup",
            body = "Starting now",
            eventTimeMs = 1000L,
            capturedAtMs = 1001L,
            isBookmarked = false
        )
        dao.insert(notification)

        dao.setBookmark("bm-test", true)

        val loaded = dao.getById("bm-test")
        assertTrue(loaded?.isBookmarked ?: false)

        val bookmarkedList = dao.observeBookmarked().first()
        assertEquals(1, bookmarkedList.size)
        assertEquals("bm-test", bookmarkedList[0].id)
    }

    @Test
    fun deleteById_removesOnlyTargetRecord() = runTest {
        val item1 = NotificationEntity(id = "item-1", packageName = "app1", eventTimeMs = 1L, capturedAtMs = 1L)
        val item2 = NotificationEntity(id = "item-2", packageName = "app2", eventTimeMs = 2L, capturedAtMs = 2L)

        dao.insertAll(listOf(item1, item2))
        val deletedCount = dao.deleteById("item-1")

        assertEquals(1, deletedCount)
        assertNull(dao.getById("item-1"))
        assertNotNull(dao.getById("item-2"))
    }

    @Test
    fun deleteAll_clearsEntireArchive() = runTest {
        val item1 = NotificationEntity(id = "item-1", packageName = "app1", eventTimeMs = 1L, capturedAtMs = 1L)
        val item2 = NotificationEntity(id = "item-2", packageName = "app2", eventTimeMs = 2L, capturedAtMs = 2L)

        dao.insertAll(listOf(item1, item2))
        dao.deleteAll()

        val all = dao.observeAll().first()
        assertTrue(all.isEmpty())
    }

    @Test
    fun deleteExpiredNonBookmarked_protectsBookmarkedRecords() = runTest {
        val cutoff = 5000L

        // Expired, not bookmarked -> should be deleted
        val expiredNormal = NotificationEntity(
            id = "expired-normal",
            packageName = "app1",
            capturedAtMs = 1000L,
            eventTimeMs = 1000L,
            isBookmarked = false
        )
        // Expired, bookmarked -> MUST BE RETAINED
        val expiredBookmarked = NotificationEntity(
            id = "expired-bookmarked",
            packageName = "app2",
            capturedAtMs = 2000L,
            eventTimeMs = 2000L,
            isBookmarked = true
        )
        // Recent, not bookmarked -> MUST BE RETAINED
        val recentNormal = NotificationEntity(
            id = "recent-normal",
            packageName = "app3",
            capturedAtMs = 6000L,
            eventTimeMs = 6000L,
            isBookmarked = false
        )

        dao.insertAll(listOf(expiredNormal, expiredBookmarked, recentNormal))

        val deletedRows = dao.deleteExpiredNonBookmarked(cutoff)
        assertEquals(1, deletedRows)

        assertNull(dao.getById("expired-normal"))
        assertNotNull(dao.getById("expired-bookmarked"))
        assertNotNull(dao.getById("recent-normal"))
    }

    @Test
    fun search_matchesTitleBodyOrAppLabel() = runTest {
        val item1 = NotificationEntity(
            id = "search-1",
            packageName = "com.app.one",
            appLabel = "Finance Tool",
            title = "Monthly Statement",
            body = "Your PDF is ready",
            eventTimeMs = 1L,
            capturedAtMs = 1L
        )
        val item2 = NotificationEntity(
            id = "search-2",
            packageName = "com.app.two",
            appLabel = "Messaging",
            title = "Doctor",
            body = "Appointment confirmation statement",
            eventTimeMs = 2L,
            capturedAtMs = 2L
        )
        val item3 = NotificationEntity(
            id = "search-3",
            packageName = "com.app.three",
            appLabel = "Social",
            title = "Friend Request",
            body = "John sent you a request",
            eventTimeMs = 3L,
            capturedAtMs = 3L
        )

        dao.insertAll(listOf(item1, item2, item3))

        val results = dao.search("Statement").first()
        assertEquals(2, results.size)
        assertTrue(results.any { it.id == "search-1" })
        assertTrue(results.any { it.id == "search-2" })
    }

    @Test
    fun filterCombined_appliesPackageDateAndQuery() = runTest {
        val item1 = NotificationEntity(
            id = "f-1",
            packageName = "com.work.chat",
            appLabel = "WorkChat",
            title = "Project Launch",
            body = "Phase 1 ready",
            eventTimeMs = 1000L,
            capturedAtMs = 1000L
        )
        val item2 = NotificationEntity(
            id = "f-2",
            packageName = "com.work.chat",
            appLabel = "WorkChat",
            title = "Lunch",
            body = "Tacos today?",
            eventTimeMs = 2000L,
            capturedAtMs = 2000L
        )
        val item3 = NotificationEntity(
            id = "f-3",
            packageName = "com.personal.chat",
            appLabel = "PersonalChat",
            title = "Project Launch",
            body = "Different app",
            eventTimeMs = 1500L,
            capturedAtMs = 1500L
        )

        dao.insertAll(listOf(item1, item2, item3))

        val filtered = dao.filterCombined(
            packageName = "com.work.chat",
            startTimeMs = 500L,
            endTimeMs = 1500L,
            query = "Project"
        ).first()

        assertEquals(1, filtered.size)
        assertEquals("f-1", filtered[0].id)
    }

    @Test
    fun findLatestBySourceKey_and_findLatestByFingerprint_returnMatchingRecord() = runTest {
        val record = NotificationEntity(
            id = "key-test",
            packageName = "com.app",
            eventTimeMs = 100L,
            capturedAtMs = 105L,
            sourceNotificationKey = "0|com.app|42|tag|1000",
            dedupeFingerprint = "fp_com.app_title_100"
        )
        dao.insert(record)

        val foundByKey = dao.findLatestBySourceKey("0|com.app|42|tag|1000")
        assertNotNull(foundByKey)
        assertEquals("key-test", foundByKey?.id)

        val foundByFp = dao.findLatestByFingerprint("fp_com.app_title_100")
        assertNotNull(foundByFp)
        assertEquals("key-test", foundByFp?.id)
    }

    @Test
    fun observeDistinctPackages_returnsSortedUniquePackages() = runTest {
        val record1 = NotificationEntity(id = "1", packageName = "com.zebra", eventTimeMs = 1L, capturedAtMs = 1L)
        val record2 = NotificationEntity(id = "2", packageName = "com.alpha", eventTimeMs = 2L, capturedAtMs = 2L)
        val record3 = NotificationEntity(id = "3", packageName = "com.zebra", eventTimeMs = 3L, capturedAtMs = 3L)
        dao.insertAll(listOf(record1, record2, record3))

        val packages = dao.observeDistinctPackages().first()
        assertEquals(listOf("com.alpha", "com.zebra"), packages)
    }
}
