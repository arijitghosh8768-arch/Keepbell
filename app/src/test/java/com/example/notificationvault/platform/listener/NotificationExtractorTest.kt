package com.example.notificationvault.platform.listener

import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.os.Process
import android.service.notification.StatusBarNotification
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NotificationExtractorTest {

    private lateinit var context: Context
    private val ownPackage = "com.example.notificationvault"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun createSbn(
        packageName: String,
        title: String? = null,
        text: String? = null,
        bigText: String? = null,
        postTime: Long = 1000L,
        category: String? = null,
        key: String = "test_key_1"
    ): StatusBarNotification {
        val extras = Bundle().apply {
            title?.let { putCharSequence(Notification.EXTRA_TITLE, it) }
            text?.let { putCharSequence(Notification.EXTRA_TEXT, it) }
            bigText?.let { putCharSequence(Notification.EXTRA_BIG_TEXT, it) }
        }

        @Suppress("DEPRECATION")
        val notification = Notification().apply {
            this.extras = extras
            this.category = category
        }

        return StatusBarNotification(
            packageName,
            packageName,
            1,
            "tag",
            1000,
            1000,
            0,
            notification,
            Process.myUserHandle(),
            postTime
        )
    }

    @Test
    fun extract_withValidFields_extractsCorrectly() {
        val extractor = NotificationExtractor(
            ownPackageName = ownPackage,
            packageManager = context.packageManager,
            currentTimeMillisProvider = { 5000L }
        )

        val sbn = createSbn(
            packageName = "org.telegram.messenger",
            title = "Alice",
            text = "Hello there",
            postTime = 1234L,
            category = "msg",
            key = "telegram_msg_1"
        )

        val candidate = extractor.extract(sbn)

        assertNotNull(candidate)
        assertEquals("org.telegram.messenger", candidate?.packageName)
        assertEquals("Alice", candidate?.title)
        assertEquals("Hello there", candidate?.body)
        assertEquals(1234L, candidate?.eventTimeMs)
        assertEquals("msg", candidate?.category)
        assertEquals(sbn.key, candidate?.sourceNotificationKey)
    }

    @Test
    fun extract_whenFromOwnPackage_returnsNull() {
        val extractor = NotificationExtractor(
            ownPackageName = ownPackage,
            packageManager = context.packageManager
        )

        val sbn = createSbn(
            packageName = ownPackage,
            title = "Notification Vault Internal",
            text = "Self alert"
        )

        val candidate = extractor.extract(sbn)
        assertNull(candidate)
    }

    @Test
    fun extract_whenExtrasAreEmpty_returnsSafeRecordWithoutCrashing() {
        val extractor = NotificationExtractor(
            ownPackageName = ownPackage,
            packageManager = context.packageManager,
            currentTimeMillisProvider = { 2000L }
        )

        val sbn = createSbn(
            packageName = "com.sample.silent",
            title = null,
            text = null,
            postTime = 1500L
        )

        val candidate = extractor.extract(sbn)

        assertNotNull(candidate)
        assertEquals("com.sample.silent", candidate?.packageName)
        assertNull(candidate?.title)
        assertNull(candidate?.body)
    }

    @Test
    fun extract_prioritizesBigTextOverStandardText() {
        val extractor = NotificationExtractor(
            ownPackageName = ownPackage,
            packageManager = context.packageManager
        )

        val sbn = createSbn(
            packageName = "com.email.app",
            title = "Important Update",
            text = "Collapsed snippet",
            bigText = "Full expanded content of the email"
        )

        val candidate = extractor.extract(sbn)

        assertNotNull(candidate)
        assertEquals("Full expanded content of the email", candidate?.body)
    }

    @Test
    fun extract_whenPostTimeIsZeroOrNegative_fallsBackToClock() {
        val extractor = NotificationExtractor(
            ownPackageName = ownPackage,
            packageManager = context.packageManager,
            currentTimeMillisProvider = { 9999L }
        )

        val sbn = createSbn(
            packageName = "com.app.timer",
            title = "Timer",
            postTime = 0L
        )

        val candidate = extractor.extract(sbn)

        assertNotNull(candidate)
        assertEquals(9999L, candidate?.eventTimeMs)
    }
}
