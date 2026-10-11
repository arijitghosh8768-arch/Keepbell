package com.example.notificationvault.platform.listener

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.notificationvault.data.local.VaultDatabase
import com.example.notificationvault.data.repository.NotificationRepositoryImpl
import com.example.notificationvault.domain.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Platform adapter service binding to Android's NotificationListenerService.
 *
 * Implements lightweight, non-blocking capture of eligible notification events.
 * Adheres strictly to the local-first policy and zero payload logging rule.
 */
class VaultNotificationListener : NotificationListenerService() {

    // Lifecycle-safe CoroutineScope tied strictly to service lifecycle (ARCHITECTURE.md §4)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val extractor by lazy {
        NotificationExtractor(
            ownPackageName = applicationContext.packageName,
            packageManager = applicationContext.packageManager
        )
    }

    // Resolves repository instance; accessible for test overrides
    var repository: NotificationRepository? = null

    private fun resolveRepository(): NotificationRepository {
        return repository ?: synchronized(this) {
            repository ?: NotificationRepositoryImpl(
                VaultDatabase.getInstance(applicationContext).notificationDao()
            ).also { repository = it }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val archivedNotification = extractor.extract(sbn) ?: return

        // Dispatch persistence asynchronously to avoid blocking platform callbacks
        serviceScope.launch {
            try {
                resolveRepository().saveNotification(archivedNotification)
            } catch (e: Exception) {
                // Sanitized handling: NEVER log notification payload text or private extras (Threat T-03)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // Per PRD FR-05 & FR-06: Dismissing a notification from the Android system shade
        // must NOT delete or mutate the archived record. This is strictly non-destructive.
    }

    override fun onDestroy() {
        // Cancel all ongoing coroutines when the service disconnects/destroys
        serviceScope.cancel()
        super.onDestroy()
    }
}
