package com.example.notificationvault.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "archived_notifications",
    indices = [
        Index(value = ["capturedAtMs"]),
        Index(value = ["packageName", "capturedAtMs"]),
        Index(value = ["isBookmarked", "capturedAtMs"]),
        Index(value = ["eventTimeMs"])
    ]
)
data class NotificationEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "packageName")
    val packageName: String,

    @ColumnInfo(name = "appLabel")
    val appLabel: String? = null,

    @ColumnInfo(name = "title")
    val title: String? = null,

    @ColumnInfo(name = "body")
    val body: String? = null,

    @ColumnInfo(name = "eventTimeMs")
    val eventTimeMs: Long,

    @ColumnInfo(name = "capturedAtMs")
    val capturedAtMs: Long,

    @ColumnInfo(name = "isBookmarked")
    val isBookmarked: Boolean = false,

    @ColumnInfo(name = "isRead")
    val isRead: Boolean = false,

    @ColumnInfo(name = "category")
    val category: String? = null,

    @ColumnInfo(name = "expiresAtMs")
    val expiresAtMs: Long? = null,

    @ColumnInfo(name = "dedupeFingerprint")
    val dedupeFingerprint: String? = null,

    @ColumnInfo(name = "sourceNotificationKey")
    val sourceNotificationKey: String? = null
)
