package com.example.notificationvault.platform.listener

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.StatusBarNotification
import com.example.notificationvault.domain.model.ArchivedNotification
import java.util.UUID

/**
 * Pure extraction logic to parse allowed fields from a platform [StatusBarNotification].
 *
 * Implements strict data minimization and self-package filtering.
 */
class NotificationExtractor(
    private val ownPackageName: String,
    private val packageManager: PackageManager? = null,
    private val currentTimeMillisProvider: () -> Long = { System.currentTimeMillis() }
) {

    /**
     * Extracts an [ArchivedNotification] snapshot from a platform notification event.
     * Returns `null` if the notification is posted by Notification Vault itself, or if invalid.
     */
    fun extract(sbn: StatusBarNotification): ArchivedNotification? {
        val packageName = sbn.packageName ?: return null

        // Self-capture prevention (ARCHITECTURE.md §4, Threat T-11)
        if (packageName == ownPackageName) {
            return null
        }

        val notification = sbn.notification ?: return null
        val extras = notification.extras

        // Best-effort application label resolution without failing on missing packages
        val appLabel = packageManager?.let { pm ->
            try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(appInfo)?.toString()
            } catch (e: Exception) {
                null
            }
        }

        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()

        // Extract body text: prioritize expanded big text, fallback to standard text
        val body = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()

        val postTime = sbn.postTime
        val eventTimeMs = if (postTime > 0L) postTime else currentTimeMillisProvider()
        val capturedAtMs = currentTimeMillisProvider()

        val sourceNotificationKey = sbn.key
        val category = notification.category

        return ArchivedNotification(
            id = UUID.randomUUID().toString(),
            packageName = packageName,
            appLabel = appLabel,
            title = title,
            body = body,
            eventTimeMs = eventTimeMs,
            capturedAtMs = capturedAtMs,
            isBookmarked = false,
            isRead = false,
            category = category,
            sourceNotificationKey = sourceNotificationKey
        )
    }
}
