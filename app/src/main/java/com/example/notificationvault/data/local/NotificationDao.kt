package com.example.notificationvault.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntity>): List<Long>

    @Update
    suspend fun update(notification: NotificationEntity): Int

    @Delete
    suspend fun delete(notification: NotificationEntity): Int

    @Query("DELETE FROM archived_notifications WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM archived_notifications")
    suspend fun deleteAll(): Int

    @Query("SELECT * FROM archived_notifications WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): NotificationEntity?

    @Query("SELECT * FROM archived_notifications WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<NotificationEntity?>

    @Query("SELECT * FROM archived_notifications ORDER BY capturedAtMs DESC")
    fun observeAll(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM archived_notifications WHERE isBookmarked = 1 ORDER BY capturedAtMs DESC")
    fun observeBookmarked(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM archived_notifications WHERE packageName = :packageName ORDER BY capturedAtMs DESC")
    fun observeByPackage(packageName: String): Flow<List<NotificationEntity>>

    @Query("""
        SELECT * FROM archived_notifications
        WHERE (title IS NOT NULL AND title LIKE '%' || :query || '%' ESCAPE '\')
           OR (body IS NOT NULL AND body LIKE '%' || :query || '%' ESCAPE '\')
           OR (appLabel IS NOT NULL AND appLabel LIKE '%' || :query || '%' ESCAPE '\')
        ORDER BY capturedAtMs DESC
    """)
    fun search(query: String): Flow<List<NotificationEntity>>

    @Query("""
        SELECT * FROM archived_notifications
        WHERE capturedAtMs BETWEEN :startTimeMs AND :endTimeMs
        ORDER BY capturedAtMs DESC
    """)
    fun filterByDateRange(startTimeMs: Long, endTimeMs: Long): Flow<List<NotificationEntity>>

    @Query("""
        SELECT * FROM archived_notifications
        WHERE (:packageName IS NULL OR packageName = :packageName)
          AND (:startTimeMs IS NULL OR capturedAtMs >= :startTimeMs)
          AND (:endTimeMs IS NULL OR capturedAtMs <= :endTimeMs)
          AND (
              :query IS NULL
              OR :query = ''
              OR (title IS NOT NULL AND title LIKE '%' || :query || '%' ESCAPE '\')
              OR (body IS NOT NULL AND body LIKE '%' || :query || '%' ESCAPE '\')
              OR (appLabel IS NOT NULL AND appLabel LIKE '%' || :query || '%' ESCAPE '\')
          )
        ORDER BY capturedAtMs DESC
    """)
    fun filterCombined(
        packageName: String? = null,
        startTimeMs: Long? = null,
        endTimeMs: Long? = null,
        query: String? = null
    ): Flow<List<NotificationEntity>>

    @Query("UPDATE archived_notifications SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun setBookmark(id: String, isBookmarked: Boolean): Int

    @Query("UPDATE archived_notifications SET isRead = :isRead WHERE id = :id")
    suspend fun setRead(id: String, isRead: Boolean): Int

    @Query("""
        DELETE FROM archived_notifications
        WHERE capturedAtMs < :cutoffMs AND isBookmarked = 0
    """)
    suspend fun deleteExpiredNonBookmarked(cutoffMs: Long): Int

    @Query("SELECT DISTINCT packageName FROM archived_notifications ORDER BY packageName ASC")
    fun observeDistinctPackages(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM archived_notifications")
    fun getCount(): Flow<Int>

    @Query("""
        SELECT * FROM archived_notifications
        WHERE sourceNotificationKey = :sourceKey
        ORDER BY capturedAtMs DESC
        LIMIT 1
    """)
    suspend fun findLatestBySourceKey(sourceKey: String): NotificationEntity?

    @Query("""
        SELECT * FROM archived_notifications
        WHERE dedupeFingerprint = :fingerprint
        ORDER BY capturedAtMs DESC
        LIMIT 1
    """)
    suspend fun findLatestByFingerprint(fingerprint: String): NotificationEntity?
}
