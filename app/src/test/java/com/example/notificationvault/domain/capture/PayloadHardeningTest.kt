package com.example.notificationvault.domain.capture

import android.app.Notification
import android.os.Bundle
import android.os.Process
import android.service.notification.StatusBarNotification
import androidx.test.core.app.ApplicationProvider
import com.example.notificationvault.data.local.VaultDatabase
import com.example.notificationvault.data.repository.CaptureSettingsRepositoryImpl
import com.example.notificationvault.data.repository.NotificationRepositoryImpl
import com.example.notificationvault.domain.capture.CaptureProcessor.Companion.takeSurrogateSafe
import com.example.notificationvault.platform.listener.NotificationExtractor
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

@RunWith(RobolectricTestRunner::class)
class PayloadHardeningTest {

    private lateinit var database: VaultDatabase
    private lateinit var notificationRepository: NotificationRepositoryImpl
    private lateinit var captureSettingsRepository: CaptureSettingsRepositoryImpl
    private lateinit var captureProcessor: CaptureProcessor

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = VaultDatabase.createInMemoryDatabase(context)
        notificationRepository = NotificationRepositoryImpl(database.notificationDao())
        captureSettingsRepository = CaptureSettingsRepositoryImpl(context)
        captureProcessor = CaptureProcessor(
            notificationRepository = notificationRepository,
            captureSettingsRepository = captureSettingsRepository,
            currentTimeMillisProvider = { 10_000L },
            duplicateWindowMs = 5_000L
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun surrogateSafeTruncation_neverSplitsUtf16SurrogatePair() {
        // High surrogate \uD83D, Low surrogate \uDE80 forms 🚀 (2 UTF-16 code units)
        val prefix = "A".repeat(255)
        val rocket = "\uD83D\uDE80"
        val input = prefix + rocket // length = 257 code units

        // If simple take(256) is used, it would include \uD83D and cut off \uDE80, leaving a dangling high surrogate.
        val truncated = input.takeSurrogateSafe(256)

        // With takeSurrogateSafe, it must back off to 255 code units, omitting the broken high surrogate.
        assertEquals(255, truncated.length)
        assertEquals(prefix, truncated)
        assertFalse(Character.isHighSurrogate(truncated.last()))
    }

    @Test
    fun surrogateSafeTruncation_whenPairFitsWithinLimit_preservesPairCompletely() {
        val prefix = "A".repeat(254)
        val rocket = "\uD83D\uDE80"
        val input = prefix + rocket // length = 256 code units

        val truncated = input.takeSurrogateSafe(256)

        assertEquals(256, truncated.length)
        assertEquals(input, truncated)
        assertTrue(truncated.endsWith(rocket))
    }

    @Test
    fun process_massiveUnicodeAndRtlContent_truncatesSafelyWithoutCrash() = runBlocking {
        // Repeated complex emoji string and Arabic RTL text
        val massiveTitle = "🔔".repeat(500)
        val massiveBody = "مرحبا بالعالم ".repeat(1000)

        val candidate = NotificationCandidate(
            packageName = "com.test.unicode",
            appLabel = "مرحبا",
            title = massiveTitle,
            body = massiveBody,
            eventTimeMs = 1000L
        )

        val result = captureProcessor.process(candidate)

        assertNotNull(result)
        assertTrue(result!!.title!!.length <= CaptureProcessor.MAX_TITLE_LENGTH)
        assertTrue(result.body!!.length <= CaptureProcessor.MAX_BODY_LENGTH)
        // Verify last character is not a dangling surrogate
        assertFalse(Character.isHighSurrogate(result.title!!.last()))
        assertFalse(Character.isHighSurrogate(result.body!!.last()))

        val stored = notificationRepository.getNotificationByIdOnce(result.id)
        assertNotNull(stored)
        assertEquals(result.title, stored?.title)
        assertEquals(result.body, stored?.body)
    }

    @Test
    fun extractor_corruptOrMalformedExtras_doesNotThrowException() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val extractor = NotificationExtractor(
            ownPackageName = "com.example.notificationvault",
            packageManager = context.packageManager
        )

        val extras = Bundle()
        // Put non-CharSequence objects in extras
        extras.putInt(Notification.EXTRA_TITLE, 12345)
        extras.putByteArray(Notification.EXTRA_TEXT, byteArrayOf(1, 2, 3))

        @Suppress("DEPRECATION")
        val notification = Notification().apply {
            this.extras = extras
        }

        val sbn = StatusBarNotification(
            "com.sample.corrupt",
            "com.sample.corrupt",
            1,
            "tag",
            1000,
            1000,
            0,
            notification,
            Process.myUserHandle(),
            1000L
        )

        // Must extract safely without ClassCastException
        val candidate = extractor.extract(sbn)
        assertNotNull(candidate)
        assertEquals("com.sample.corrupt", candidate?.packageName)
    }

    @Test
    fun process_burstConcurrentCallbacks_persistsAllOrDeduplicatesDeterministically() = runBlocking {
        val count = 50
        val deferredList = (1..count).map { i ->
            async {
                val candidate = NotificationCandidate(
                    packageName = "com.concurrent.app",
                    appLabel = "ConcurrentApp",
                    title = "Alert #$i",
                    body = "Unique message body $i",
                    eventTimeMs = 1000L + i,
                    sourceNotificationKey = "key_$i"
                )
                captureProcessor.process(candidate)
            }
        }

        val results = deferredList.awaitAll()
        assertEquals(count, results.filterNotNull().size)

        // Verify database holds all 50 distinct records without corruption
        val totalCount = notificationRepository.getNotificationCount().first()
        assertEquals(count, totalCount)
    }
}
