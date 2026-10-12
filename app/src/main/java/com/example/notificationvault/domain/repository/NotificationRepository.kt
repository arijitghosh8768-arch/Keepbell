package com.example.notificationvault.domain.repository

import com.example.notificationvault.domain.model.ArchivedNotification
import kotlinx.coroutines.flow.Flow

/**
 * Domain interface for notification archive storage operations.
 */
interface NotificationRepository {

    /**
     * Observes all archived notifications sorted newest-first by capture time.
     */
    fun getNotifications(): Flow<List<ArchivedNotification>>

    /**
     * Observes only notifications that the user has bookmarked.
     */
    fun getBookmarkedNotifications(): Flow<List<ArchivedNotification>>

    /**
     * Observes notifications originating from a specific package.
     */
    fun getNotificationsByPackage(packageName: String): Flow<List<ArchivedNotification>>

    /**
     * Observes a single notification record by its UUID.
     */
    fun getNotificationById(id: String): Flow<ArchivedNotification?>

    /**
     * Retrieves a single notification record once.
     */
    suspend fun getNotificationByIdOnce(id: String): ArchivedNotification?

    /**
     * Searches notifications matching title, body, or application label.
     */
    fun searchNotifications(query: String): Flow<List<ArchivedNotification>>

    /**
     * Filters notifications combining package name, timestamp boundaries, and search query.
     */
    fun filterNotifications(
        packageName: String? = null,
        startTimeMs: Long? = null,
        endTimeMs: Long? = null,
        query: String? = null
    ): Flow<List<ArchivedNotification>>

    /**
     * Persists a single archived notification.
     */
    suspend fun saveNotification(notification: ArchivedNotification): Long

    /**
     * Persists a batch of archived notifications.
     */
    suspend fun saveNotifications(notifications: List<ArchivedNotification>): List<Long>

    /**
     * Updates an existing notification.
     */
    suspend fun updateNotification(notification: ArchivedNotification): Int

    /**
     * Toggles or sets bookmark status for a record.
     */
    suspend fun setBookmark(id: String, isBookmarked: Boolean): Int

    /**
     * Sets read status for a record in Notification Vault.
     */
    suspend fun setRead(id: String, isRead: Boolean): Int

    /**
     * Deletes a single notification record by ID.
     */
    suspend fun deleteNotification(id: String): Int

    /**
     * Clears all archived notifications.
     */
    suspend fun deleteAllNotifications(): Int

    /**
     * Deletes expired non-bookmarked records older than cutoff time.
     */
    suspend fun deleteExpiredNotifications(cutoffMs: Long): Int

    /**
     * Looks up the latest stored record for an ephemeral platform notification key.
     */
    suspend fun findBySourceKey(sourceKey: String): ArchivedNotification?

    /**
     * Looks up the latest stored record matching a deduplication fingerprint.
     */
    suspend fun findByFingerprint(fingerprint: String): ArchivedNotification?

    /**
     * Observes the list of distinct source application packages recorded in the archive.
     */
    fun getDistinctPackages(): Flow<List<String>>

    /**
     * Observes the total count of archived notifications.
     */
    fun getNotificationCount(): Flow<Int>
}
