package com.example.notificationvault.platform

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NotificationAccessCheckerTest {

    private lateinit var context: Context
    private lateinit var checker: NotificationAccessChecker

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        checker = NotificationAccessChecker(context)
    }

    @Test
    fun isNotificationAccessGranted_whenNotEnabledInSettings_returnsFalse() {
        val isGranted = checker.isNotificationAccessGranted()
        assertFalse(isGranted)
    }

    @Test
    fun createSettingsIntent_targetsNotificationListenerAction() {
        val intent = checker.createSettingsIntent()

        assertNotNull(intent)
        assertEquals(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS, intent.action)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }
}
