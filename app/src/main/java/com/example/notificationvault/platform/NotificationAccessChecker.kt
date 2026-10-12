package com.example.notificationvault.platform

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/**
 * Utility to verify whether the system notification listener access is enabled
 * and build the intent to navigate the user to Android's Notification Access settings.
 */
open class NotificationAccessChecker(private val context: Context) {

    /**
     * Checks whether the current app package is present in the list of enabled
     * notification listener packages authorized by the user in system settings.
     */
    open fun isNotificationAccessGranted(): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackages.contains(context.packageName)
    }

    /**
     * Creates an intent that opens the system Notification Listener Settings screen.
     */
    fun createSettingsIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
