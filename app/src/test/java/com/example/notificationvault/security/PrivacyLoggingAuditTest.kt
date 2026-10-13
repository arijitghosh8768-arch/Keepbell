package com.example.notificationvault.security

import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.os.Process
import android.service.notification.StatusBarNotification
import androidx.test.core.app.ApplicationProvider
import com.example.notificationvault.data.local.VaultDatabase
import com.example.notificationvault.data.repository.CaptureSettingsRepositoryImpl
import com.example.notificationvault.data.repository.NotificationRepositoryImpl
import com.example.notificationvault.domain.capture.CaptureProcessor
import com.example.notificationvault.platform.listener.VaultNotificationListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ServiceController
import org.robolectric.shadows.ShadowLog
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.PrintStream

/**
 * Validates TC-025: Notification payload logging audit.
 *
 * Verifies that sensitive notification text (sentinel strings, OTPs, private messages)
 * processed through the capture pipeline, Room repository, and listener callbacks
 * are never logged to Logcat, standard output, or standard error.
 */
@RunWith(RobolectricTestRunner::class)
class PrivacyLoggingAuditTest {

    private lateinit var context: Context
    private lateinit var database: VaultDatabase
    private lateinit var repository: NotificationRepositoryImpl
    private lateinit var captureSettingsRepository: CaptureSettingsRepositoryImpl
    private lateinit var captureProcessor: CaptureProcessor
    private lateinit var serviceController: ServiceController<VaultNotificationListener>
    private lateinit var listenerService: VaultNotificationListener

    private val originalOut = System.out
    private val originalErr = System.err
    private val stdoutCapture = ByteArrayOutputStream()
    private val stderrCapture = ByteArrayOutputStream()

    companion object {
        const val SENTINEL_TITLE = "PRIVATE_SENTINEL_DO_NOT_LOG_8472"
        const val SENTINEL_BODY = "Confidential OTP code 491028 for account verification"
        const val SENTINEL_EXTRA = "SECRET_EXTRA_DATA_VALUE_99182"
    }

    private class TeeOutputStream(
        private val primary: OutputStream,
        private val copy: OutputStream
    ) : OutputStream() {
        override fun write(b: Int) {
            primary.write(b)
            copy.write(b)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            primary.write(b, off, len)
            copy.write(b, off, len)
        }

        override fun flush() {
            primary.flush()
            copy.flush()
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = VaultDatabase.createInMemoryDatabase(context)
        repository = NotificationRepositoryImpl(database.notificationDao(), Dispatchers.IO)
        captureSettingsRepository = CaptureSettingsRepositoryImpl(context, Dispatchers.IO)
        captureProcessor = CaptureProcessor(repository, captureSettingsRepository)

        serviceController = Robolectric.buildService(VaultNotificationListener::class.java).create()
        listenerService = serviceController.get()
        listenerService.repository = repository
        listenerService.captureProcessor = captureProcessor

        // Intercept standard streams without breaking normal output to test runners
        System.setOut(PrintStream(TeeOutputStream(originalOut, stdoutCapture)))
        System.setErr(PrintStream(TeeOutputStream(originalErr, stderrCapture)))

        // Clear Robolectric logcat logs
        ShadowLog.clear()
    }

    @After
    fun tearDown() {
        System.setOut(originalOut)
        System.setErr(originalErr)
        serviceController.destroy()
        database.close()
    }

    private fun createSentinelSbn(packageName: String): StatusBarNotification {
        val extras = Bundle().apply {
            putCharSequence(Notification.EXTRA_TITLE, SENTINEL_TITLE)
            putCharSequence(Notification.EXTRA_TEXT, SENTINEL_BODY)
            putString("custom_private_extra", SENTINEL_EXTRA)
        }
        @Suppress("DEPRECATION")
        val notification = Notification().apply { this.extras = extras }

        return StatusBarNotification(
            packageName, packageName, 42, "tag_sentinel", 1000, 1000, 0,
            notification, Process.myUserHandle(), 1000L
        )
    }

    private fun assertSentinelAbsentFromAllLogSinks() {
        // 1. Inspect Robolectric Logcat log items across all priorities (V, D, I, W, E, WTF)
        val logs = ShadowLog.getLogs()
        for (logItem in logs) {
            val message = logItem.msg ?: ""
            val tag = logItem.tag ?: ""
            assertFalse(
                "Sentinel title must not appear in Logcat (tag: $tag)",
                message.contains(SENTINEL_TITLE) || tag.contains(SENTINEL_TITLE)
            )
            assertFalse(
                "Sentinel body must not appear in Logcat (tag: $tag)",
                message.contains(SENTINEL_BODY) || tag.contains(SENTINEL_BODY)
            )
            assertFalse(
                "Sentinel extra must not appear in Logcat (tag: $tag)",
                message.contains(SENTINEL_EXTRA) || tag.contains(SENTINEL_EXTRA)
            )
        }

        // 2. Inspect captured stdout (println)
        val stdoutString = stdoutCapture.toString()
        assertFalse("Sentinel title must not appear in stdout", stdoutString.contains(SENTINEL_TITLE))
        assertFalse("Sentinel body must not appear in stdout", stdoutString.contains(SENTINEL_BODY))
        assertFalse("Sentinel extra must not appear in stdout", stdoutString.contains(SENTINEL_EXTRA))

        // 3. Inspect captured stderr (printStackTrace)
        val stderrString = stderrCapture.toString()
        assertFalse("Sentinel title must not appear in stderr", stderrString.contains(SENTINEL_TITLE))
        assertFalse("Sentinel body must not appear in stderr", stderrString.contains(SENTINEL_BODY))
        assertFalse("Sentinel extra must not appear in stderr", stderrString.contains(SENTINEL_EXTRA))
    }

    @Test
    fun capturePipeline_processesSentinelNotification_withoutEmittingPayloadToLogs() = runBlocking {
        val sbn = createSentinelSbn("com.secure.chat")

        // Exercise normal capture flow
        listenerService.onNotificationPosted(sbn)

        // Await async Room persistence on Dispatchers.IO
        var storedItem = repository.getNotifications().first().find { it.title == SENTINEL_TITLE }
        var retries = 0
        while (storedItem == null && retries < 20) {
            Thread.sleep(50)
            storedItem = repository.getNotifications().first().find { it.title == SENTINEL_TITLE }
            retries++
        }

        // Verify notification was indeed processed and stored locally in Room database
        assertNotNull("Sentinel notification must be stored locally in the database", storedItem)

        // Verify sentinels are completely absent from all logging sinks
        assertSentinelAbsentFromAllLogSinks()
    }

    @Test
    fun capturePipeline_excludedPackage_doesNotEmitPayloadToLogs() = runBlocking {
        // Exclude target package
        captureSettingsRepository.setPackageExcluded("com.excluded.app", true)

        val sbn = createSentinelSbn("com.excluded.app")

        // Exercise exclusion evaluation flow
        listenerService.onNotificationPosted(sbn)
        Thread.sleep(100)

        // Must not be saved
        val notifications = repository.getNotifications().first()
        assertTrue("Excluded notification must not be saved", notifications.isEmpty())

        // Must not leak payload while logging exclusion decisions
        assertSentinelAbsentFromAllLogSinks()
    }

    @Test
    fun capturePipeline_databaseFailure_doesNotEmitPayloadToLogs() = runBlocking {
        // Create repository test double that throws on save
        val failingRepo = object : com.example.notificationvault.domain.repository.NotificationRepository by repository {
            override suspend fun saveNotification(notification: com.example.notificationvault.domain.model.ArchivedNotification): Long {
                throw android.database.sqlite.SQLiteDiskIOException("Disk full failure while saving notification with id ${notification.id}")
            }
        }
        val failingProcessor = CaptureProcessor(failingRepo, captureSettingsRepository)
        listenerService.repository = failingRepo
        listenerService.captureProcessor = failingProcessor

        val sbn = createSentinelSbn("com.failure.test")

        // Exercise failure path
        listenerService.onNotificationPosted(sbn)
        Thread.sleep(100)

        // Verify sentinels are completely absent from all logs even on error
        assertSentinelAbsentFromAllLogSinks()
    }
}
