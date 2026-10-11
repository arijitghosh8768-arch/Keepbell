package com.example.notificationvault.platform.listener

import android.app.Notification
import android.os.Bundle
import android.os.Process
import android.service.notification.StatusBarNotification
import com.example.notificationvault.data.local.VaultDatabase
import com.example.notificationvault.data.repository.NotificationRepositoryImpl
import com.example.notificationvault.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ServiceController

@RunWith(RobolectricTestRunner::class)
class VaultNotificationListenerTest {

    private lateinit var controller: ServiceController<VaultNotificationListener>
    private lateinit var service: VaultNotificationListener
    private lateinit var database: VaultDatabase
    private lateinit var repository: NotificationRepository

    @Before
    fun setUp() {
        controller = Robolectric.buildService(VaultNotificationListener::class.java).create()
        service = controller.get()

        database = VaultDatabase.createInMemoryDatabase(service.applicationContext)
        repository = NotificationRepositoryImpl(database.notificationDao())
        service.repository = repository
    }

    @After
    fun tearDown() {
        database.close()
        controller.destroy()
    }

    private fun createSbn(
        packageName: String,
        title: String? = null,
        text: String? = null,
        key: String = "key_1"
    ): StatusBarNotification {
        val extras = Bundle().apply {
            title?.let { putCharSequence(Notification.EXTRA_TITLE, it) }
            text?.let { putCharSequence(Notification.EXTRA_TEXT, it) }
        }

        @Suppress("DEPRECATION")
        val notification = Notification().apply {
            this.extras = extras
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
            System.currentTimeMillis()
        )
    }

    @Test
    fun onNotificationPosted_persistsEligibleNotification() = runBlocking {
        val sbn = createSbn(
            packageName = "org.telegram.messenger",
            title = "Bob",
            text = "Meeting confirmed",
            key = "telegram_1"
        )

        service.onNotificationPosted(sbn)

        // Allow async persistence on IO dispatcher
        Thread.sleep(100)

        val notifications = repository.getNotifications().first()
        assertEquals(1, notifications.size)
        assertEquals("org.telegram.messenger", notifications[0].packageName)
        assertEquals("Bob", notifications[0].title)
        assertEquals("Meeting confirmed", notifications[0].body)
    }

    @Test
    fun onNotificationPosted_ignoresOwnPackageNotifications() = runBlocking {
        val ownPackage = service.applicationContext.packageName
        val sbn = createSbn(
            packageName = ownPackage,
            title = "Vault Service Alert",
            text = "Self loop prevention"
        )

        service.onNotificationPosted(sbn)
        Thread.sleep(100)

        val notifications = repository.getNotifications().first()
        assertTrue(notifications.isEmpty())
    }

    @Test
    fun onNotificationRemoved_doesNotDeleteArchivedRecord() = runBlocking {
        // Pre-save an archived notification
        val sbn = createSbn(
            packageName = "com.banking.app",
            title = "Transfer Alert",
            text = "Payment received"
        )
        service.onNotificationPosted(sbn)
        Thread.sleep(100)

        assertEquals(1, repository.getNotifications().first().size)

        // System shade removal event occurs
        service.onNotificationRemoved(sbn)
        Thread.sleep(100)

        // Archive record must remain intact (FR-05, FR-06)
        val remaining = repository.getNotifications().first()
        assertEquals(1, remaining.size)
        assertEquals("Transfer Alert", remaining[0].title)
    }

    @Test
    fun onNotificationPosted_withSentinelString_processesSafelyWithoutLogLeaks() = runBlocking {
        val sentinelString = "PRIVATE_SENTINEL_DO_NOT_LOG_8472"
        val sbn = createSbn(
            packageName = "com.secure.chat",
            title = "Secret Sender",
            text = sentinelString
        )

        service.onNotificationPosted(sbn)
        Thread.sleep(100)

        val stored = repository.getNotifications().first()
        assertEquals(1, stored.size)
        assertEquals(sentinelString, stored[0].body)
    }
}
