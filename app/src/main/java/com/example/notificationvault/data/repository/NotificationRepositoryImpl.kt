package com.example.notificationvault.data.repository

import com.example.notificationvault.data.local.NotificationDao
import com.example.notificationvault.data.local.toDomain
import com.example.notificationvault.data.local.toEntity
import com.example.notificationvault.domain.model.ArchivedNotification
import com.example.notificationvault.domain.repository.NotificationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository implementation delegating persistence to Room [NotificationDao].
 *
 * All database operations are dispatched to an IO dispatcher to guarantee the main thread is never blocked.
 */
class NotificationRepositoryImpl(
    private val notificationDao: NotificationDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : NotificationRepository {

    override fun getNotifications(): Flow<List<ArchivedNotification>> {
        return notificationDao.observeAll()
            .map { entities -> entities.toDomain() }
            .flowOn(ioDispatcher)
    }

    override fun getBookmarkedNotifications(): Flow<List<ArchivedNotification>> {
        return notificationDao.observeBookmarked()
            .map { entities -> entities.toDomain() }
            .flowOn(ioDispatcher)
    }

    override fun getNotificationsByPackage(packageName: String): Flow<List<ArchivedNotification>> {
        return notificationDao.observeByPackage(packageName)
            .map { entities -> entities.toDomain() }
            .flowOn(ioDispatcher)
    }

    override fun getNotificationById(id: String): Flow<ArchivedNotification?> {
        return notificationDao.observeById(id)
            .map { entity -> entity?.toDomain() }
            .flowOn(ioDispatcher)
    }

    override suspend fun getNotificationByIdOnce(id: String): ArchivedNotification? {
        return withContext(ioDispatcher) {
            notificationDao.getById(id)?.toDomain()
        }
    }

    override fun searchNotifications(query: String): Flow<List<ArchivedNotification>> {
        val sanitizedQuery = query.trim()
        return if (sanitizedQuery.isEmpty()) {
            getNotifications()
        } else {
            notificationDao.search(sanitizedQuery)
                .map { entities -> entities.toDomain() }
                .flowOn(ioDispatcher)
        }
    }

    override fun filterNotifications(
        packageName: String?,
        startTimeMs: Long?,
        endTimeMs: Long?,
        query: String?
    ): Flow<List<ArchivedNotification>> {
        val sanitizedQuery = query?.trim()?.takeIf { it.isNotEmpty() }
        val sanitizedPackage = packageName?.trim()?.takeIf { it.isNotEmpty() }

        return notificationDao.filterCombined(
            packageName = sanitizedPackage,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            query = sanitizedQuery
        )
            .map { entities -> entities.toDomain() }
            .flowOn(ioDispatcher)
    }

    override suspend fun saveNotification(notification: ArchivedNotification): Long {
        return withContext(ioDispatcher) {
            notificationDao.insert(notification.toEntity())
        }
    }

    override suspend fun saveNotifications(notifications: List<ArchivedNotification>): List<Long> {
        return withContext(ioDispatcher) {
            notificationDao.insertAll(notifications.map { it.toEntity() })
        }
    }

    override suspend fun updateNotification(notification: ArchivedNotification): Int {
        return withContext(ioDispatcher) {
            notificationDao.update(notification.toEntity())
        }
    }

    override suspend fun setBookmark(id: String, isBookmarked: Boolean): Int {
        return withContext(ioDispatcher) {
            notificationDao.setBookmark(id, isBookmarked)
        }
    }

    override suspend fun setRead(id: String, isRead: Boolean): Int {
        return withContext(ioDispatcher) {
            notificationDao.setRead(id, isRead)
        }
    }

    override suspend fun deleteNotification(id: String): Int {
        return withContext(ioDispatcher) {
            notificationDao.deleteById(id)
        }
    }

    override suspend fun deleteAllNotifications(): Int {
        return withContext(ioDispatcher) {
            notificationDao.deleteAll()
        }
    }

    override suspend fun deleteExpiredNotifications(cutoffMs: Long): Int {
        return withContext(ioDispatcher) {
            notificationDao.deleteExpiredNonBookmarked(cutoffMs)
        }
    }

    override suspend fun findBySourceKey(sourceKey: String): ArchivedNotification? {
        return withContext(ioDispatcher) {
            notificationDao.findLatestBySourceKey(sourceKey)?.toDomain()
        }
    }

    override suspend fun findByFingerprint(fingerprint: String): ArchivedNotification? {
        return withContext(ioDispatcher) {
            notificationDao.findLatestByFingerprint(fingerprint)?.toDomain()
        }
    }

    override fun getNotificationCount(): Flow<Int> {
        return notificationDao.getCount()
            .flowOn(ioDispatcher)
    }
}
