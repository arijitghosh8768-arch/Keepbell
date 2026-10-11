package com.example.notificationvault.domain.capture

import com.example.notificationvault.data.local.VaultDatabase
import com.example.notificationvault.data.repository.CaptureSettingsRepositoryImpl
import com.example.notificationvault.data.repository.NotificationRepositoryImpl
import com.example.notificationvault.domain.repository.NotificationRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CaptureProcessorTest {

    private lateinit var database: VaultDatabase
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var captureSettingsRepository: CaptureSettingsRepository
    private lateinit var captureProcessor: CaptureProcessor

    private var fakeCurrentTime = 10_000L

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        database = VaultDatabase.createInMemoryDatabase(context)
        notificationRepository = NotificationRepositoryImpl(database.notificationDao())
        captureSettingsRepository = CaptureSettingsRepositoryImpl(context)
        fakeCurrentTime = 10_000L
        captureProcessor = CaptureProcessor(
            notificationRepository = notificationRepository,
            captureSettingsRepository = captureSettingsRepository,
            currentTimeMillisProvider = { fakeCurrentTime },
            duplicateWindowMs = 5_000L
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun process_whenGlobalCaptureDisabled_doesNotPersist() = runBlocking {
        captureSettingsRepository.setGlobalCaptureEnabled(false)

        val candidate = NotificationCandidate(
            packageName = "com.test.app",
            title = "Alert",
            body = "Important message",
            eventTimeMs = 1000L
        )

        val result = captureProcessor.process(candidate)

        assertNull(result)
        assertTrue(notificationRepository.getNotifications().first().isEmpty())
    }

    @Test
    fun process_whenPackageExcluded_doesNotPersist() = runBlocking {
        captureSettingsRepository.setPackageExcluded("com.test.spam", true)

        val candidate = NotificationCandidate(
            packageName = "com.test.spam",
            title = "Promo",
            body = "Discount offer",
            eventTimeMs = 1000L
        )

        val result = captureProcessor.process(candidate)

        assertNull(result)
        assertTrue(notificationRepository.getNotifications().first().isEmpty())
    }

    @Test
    fun process_enforcesFieldCapsAndTruncation() = runBlocking {
        val longPackage = "a".repeat(300)
        val longAppLabel = "b".repeat(200)
        val longTitle = "c".repeat(400)
        val longBody = "d".repeat(5000)
        val longCategory = "e".repeat(100)

        val candidate = NotificationCandidate(
            packageName = longPackage,
            appLabel = longAppLabel,
            title = longTitle,
            body = longBody,
            eventTimeMs = 1000L,
            category = longCategory
        )

        val result = captureProcessor.process(candidate)

        assertNotNull(result)
        assertEquals(256, result?.packageName?.length)
        assertEquals(128, result?.appLabel?.length)
        assertEquals(256, result?.title?.length)
        assertEquals(4096, result?.body?.length)
        assertEquals(64, result?.category?.length)
    }

    @Test
    fun process_blankValuesAreNormalizedToNull() = runBlocking {
        val candidate = NotificationCandidate(
            packageName = "com.test.blank",
            appLabel = "   ",
            title = "   ",
            body = "\n\t  ",
            eventTimeMs = 1000L,
            category = "   "
        )

        val result = captureProcessor.process(candidate)

        assertNotNull(result)
        assertEquals("com.test.blank", result?.packageName)
        assertNull(result?.appLabel)
        assertNull(result?.title)
        assertNull(result?.body)
        assertNull(result?.category)
    }

    @Test
    fun process_withMatchingSourceKey_updatesExistingRecordInPlaceAndPreservesBookmarkAndReadState() = runBlocking {
        val key = "0|com.chat.app|1|tag|100"

        // 1. Initial post
        val candidate1 = NotificationCandidate(
            packageName = "com.chat.app",
            appLabel = "ChatApp",
            title = "Chat Group",
            body = "Bob: Hello",
            eventTimeMs = 1000L,
            sourceNotificationKey = key
        )
        val saved1 = captureProcessor.process(candidate1)
        assertNotNull(saved1)
        val initialId = saved1!!.id

        // Mark as bookmarked and read by user
        notificationRepository.setBookmark(initialId, true)
        notificationRepository.setRead(initialId, true)

        val initialStored = notificationRepository.getNotificationByIdOnce(initialId)
        assertTrue(initialStored?.isBookmarked == true)
        assertTrue(initialStored?.isRead == true)

        // 2. Updated notification arrives under the same key
        fakeCurrentTime = 12_000L
        val candidate2 = NotificationCandidate(
            packageName = "com.chat.app",
            appLabel = "ChatApp",
            title = "Chat Group",
            body = "Bob: How are you?",
            eventTimeMs = 2000L,
            sourceNotificationKey = key
        )
        val saved2 = captureProcessor.process(candidate2)

        assertNotNull(saved2)
        assertEquals(initialId, saved2?.id)
        assertEquals("Bob: How are you?", saved2?.body)
        assertEquals(2000L, saved2?.eventTimeMs)
        assertEquals(12_000L, saved2?.capturedAtMs)

        // Verify database contains exactly 1 row, and user states were strictly preserved
        val all = notificationRepository.getNotifications().first()
        assertEquals(1, all.size)
        assertEquals(initialId, all[0].id)
        assertTrue(all[0].isBookmarked)
        assertTrue(all[0].isRead)
        assertEquals("Bob: How are you?", all[0].body)
    }

    @Test
    fun process_missingSourceKey_rapidBurstWithinWindow_deduplicatesWithoutNewRow() = runBlocking {
        fakeCurrentTime = 10_000L
        val candidate1 = NotificationCandidate(
            packageName = "com.weather.app",
            title = "Rain Alert",
            body = "Light rain expected",
            eventTimeMs = 1000L,
            sourceNotificationKey = null
        )
        val saved1 = captureProcessor.process(candidate1)
        assertNotNull(saved1)
        val firstId = saved1!!.id

        // Burst 2 seconds later (within 5_000ms window)
        fakeCurrentTime = 12_000L
        val candidate2 = NotificationCandidate(
            packageName = "com.weather.app",
            title = "Rain Alert",
            body = "Light rain expected",
            eventTimeMs = 1200L,
            sourceNotificationKey = null
        )
        val saved2 = captureProcessor.process(candidate2)
        assertNotNull(saved2)
        assertEquals(firstId, saved2?.id)

        // Only 1 row persisted
        val all = notificationRepository.getNotifications().first()
        assertEquals(1, all.size)
        assertEquals(12_000L, all[0].capturedAtMs)
    }

    @Test
    fun process_missingSourceKey_outsideWindow_createsDistinctLegitimateAlert() = runBlocking {
        fakeCurrentTime = 10_000L
        val candidate1 = NotificationCandidate(
            packageName = "com.alarm.clock",
            title = "Alarm",
            body = "Time to wake up",
            eventTimeMs = 1000L,
            sourceNotificationKey = null
        )
        val saved1 = captureProcessor.process(candidate1)
        assertNotNull(saved1)

        // Second alert 20 seconds later (outside 5_000ms window)
        fakeCurrentTime = 30_000L
        val candidate2 = NotificationCandidate(
            packageName = "com.alarm.clock",
            title = "Alarm",
            body = "Time to wake up",
            eventTimeMs = 21000L,
            sourceNotificationKey = null
        )
        val saved2 = captureProcessor.process(candidate2)
        assertNotNull(saved2)

        // Both distinct legitimate alerts preserved
        val all = notificationRepository.getNotifications().first()
        assertEquals(2, all.size)
        assertTrue(all[0].id != all[1].id)
    }

    @Test
    fun process_disablingPackageCapture_doesNotDeleteExistingRecords() = runBlocking {
        val candidate = NotificationCandidate(
            packageName = "com.finance.bank",
            title = "Security Code",
            body = "Code is 123456",
            eventTimeMs = 1000L
        )
        captureProcessor.process(candidate)
        assertEquals(1, notificationRepository.getNotifications().first().size)

        // User now disables capture for this package
        captureSettingsRepository.setPackageExcluded("com.finance.bank", true)

        // Prior archive records must NEVER be deleted
        val remaining = notificationRepository.getNotifications().first()
        assertEquals(1, remaining.size)
        assertEquals("com.finance.bank", remaining[0].packageName)
    }

    @Test
    fun process_concurrentCallbacks_serializesSafely() = runBlocking {
        val candidates = (1..10).map { i ->
            NotificationCandidate(
                packageName = "com.concurrent.app",
                title = "Item $i",
                body = "Body $i",
                eventTimeMs = 1000L + i,
                sourceNotificationKey = "key_$i"
            )
        }

        // Launch concurrently
        val deferreds = candidates.map { candidate ->
            async {
                captureProcessor.process(candidate)
            }
        }
        val results = deferreds.awaitAll()

        assertEquals(10, results.filterNotNull().size)
        assertEquals(10, notificationRepository.getNotifications().first().size)
    }
}
