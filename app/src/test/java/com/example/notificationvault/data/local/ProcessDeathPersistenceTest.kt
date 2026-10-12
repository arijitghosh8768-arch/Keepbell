package com.example.notificationvault.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.notificationvault.data.repository.CaptureSettingsRepositoryImpl
import com.example.notificationvault.data.repository.NotificationRepositoryImpl
import com.example.notificationvault.data.repository.RetentionSettingsRepositoryImpl
import com.example.notificationvault.domain.model.ArchivedNotification
import com.example.notificationvault.domain.policy.RetentionPolicy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Validates persistent storage survival across simulated process termination and cold re-instantiation.
 * Uses real file-backed SQLite database storage and Android SharedPreferences.
 */
@RunWith(RobolectricTestRunner::class)
class ProcessDeathPersistenceTest {

    private lateinit var context: Context
    private val dbName = "notification_vault_death_test.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        // Clean up database file reliably
        context.deleteDatabase(dbName)

        // Clear preferences
        val capturePrefs = context.getSharedPreferences(CaptureSettingsRepositoryImpl.PREFS_NAME, Context.MODE_PRIVATE)
        capturePrefs.edit().clear().commit()
        val retentionPrefs = context.getSharedPreferences(RetentionSettingsRepositoryImpl.PREFS_NAME, Context.MODE_PRIVATE)
        retentionPrefs.edit().clear().commit()
    }

    @Test
    fun databaseFile_persistsEntitiesBookmarksAndReadFlagsAcrossInstanceRecreation() = runBlocking {
        // --- Process 1: Initial startup, population, and user modifications ---
        val dbInstance1 = Room.databaseBuilder(context, VaultDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        var repoInstance1 = NotificationRepositoryImpl(dbInstance1.notificationDao())

        val notif1 = ArchivedNotification(
            id = "notif_persist_1",
            packageName = "com.banking.app",
            appLabel = "Bank",
            title = "Debit Alert",
            body = "Amount deducted: $50",
            eventTimeMs = 1000L,
            capturedAtMs = 1000L,
            isBookmarked = true,
            isRead = true
        )
        val notif2 = ArchivedNotification(
            id = "notif_persist_2",
            packageName = "com.chat.app",
            appLabel = "Messenger",
            title = "Alice",
            body = "See you soon!",
            eventTimeMs = 2000L,
            capturedAtMs = 2000L,
            isBookmarked = false,
            isRead = false
        )

        repoInstance1.saveNotification(notif1)
        repoInstance1.saveNotification(notif2)

        // Verify state prior to process destruction
        assertEquals(2, repoInstance1.getNotificationCount().first())

        // Simulate process death: completely close SQLite database connections and drop in-memory references
        dbInstance1.close()

        // --- Process 2: App cold restart from SQLite file ---
        val dbInstance2 = Room.databaseBuilder(context, VaultDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        val repoInstance2 = NotificationRepositoryImpl(dbInstance2.notificationDao())

        // Verify total count and individual entity integrity
        assertEquals(2, repoInstance2.getNotificationCount().first())

        val loaded1 = repoInstance2.getNotificationByIdOnce("notif_persist_1")
        assertNotNull(loaded1)
        assertEquals("com.banking.app", loaded1?.packageName)
        assertEquals("Debit Alert", loaded1?.title)
        assertEquals("Amount deducted: $50", loaded1?.body)
        assertEquals(1000L, loaded1?.eventTimeMs)
        assertEquals(1000L, loaded1?.capturedAtMs)
        assertTrue("Bookmark flag must survive process death", loaded1?.isBookmarked == true)
        assertTrue("Read flag must survive process death", loaded1?.isRead == true)

        val loaded2 = repoInstance2.getNotificationByIdOnce("notif_persist_2")
        assertNotNull(loaded2)
        assertEquals("com.chat.app", loaded2?.packageName)
        assertFalse("Unbookmarked item must remain unbookmarked", loaded2?.isBookmarked == true)
        assertFalse("Unread item must remain unread", loaded2?.isRead == true)

        // Verify Room reactive query flows against restored file
        val bookmarkedList = repoInstance2.getBookmarkedNotifications().first()
        assertEquals(1, bookmarkedList.size)
        assertEquals("notif_persist_1", bookmarkedList[0].id)

        val filteredByPkg = repoInstance2.filterNotifications(packageName = "com.banking.app").first()
        assertEquals(1, filteredByPkg.size)
        assertEquals("notif_persist_1", filteredByPkg[0].id)

        dbInstance2.close()
    }

    @Test
    fun sharedPreferences_persistsCaptureAndRetentionSettingsAcrossRepositoryReinstantiation() = runBlocking {
        // --- Process 1: User customizes capture toggles and retention policy ---
        var captureRepo1 = CaptureSettingsRepositoryImpl(context)
        var retentionRepo1 = RetentionSettingsRepositoryImpl(context)

        captureRepo1.setGlobalCaptureEnabled(false)
        captureRepo1.setPackageExcluded("com.promo.spam", true)
        captureRepo1.setPackageExcluded("com.game.notifications", true)
        retentionRepo1.setRetentionPolicy(RetentionPolicy.KEEP_UNTIL_DELETED)

        assertFalse(captureRepo1.getGlobalCaptureEnabled())
        assertEquals(RetentionPolicy.KEEP_UNTIL_DELETED, retentionRepo1.getRetentionPolicy())

        // Simulate process termination: discard repository references and instantiate new ones
        var captureRepo2 = CaptureSettingsRepositoryImpl(context)
        var retentionRepo2 = RetentionSettingsRepositoryImpl(context)

        // --- Process 2: Cold restart restores persisted preferences ---
        assertFalse("Global capture disabled state must persist", captureRepo2.getGlobalCaptureEnabled())
        assertTrue("Excluded packages must persist", captureRepo2.isPackageExcluded("com.promo.spam"))
        assertTrue("Excluded packages must persist", captureRepo2.isPackageExcluded("com.game.notifications"))
        assertFalse("Non-excluded packages must remain unexcluded", captureRepo2.isPackageExcluded("com.important.mail"))

        assertEquals("Retention policy must persist KEEP_UNTIL_DELETED", RetentionPolicy.KEEP_UNTIL_DELETED, retentionRepo2.getRetentionPolicy())

        // Verify update in second session
        retentionRepo2.setRetentionPolicy(RetentionPolicy.DAYS_7)
        val retentionRepo3 = RetentionSettingsRepositoryImpl(context)
        assertEquals("Updated policy must persist", RetentionPolicy.DAYS_7, retentionRepo3.getRetentionPolicy())
    }
}
