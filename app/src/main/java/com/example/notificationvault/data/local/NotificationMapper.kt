package com.example.notificationvault.data.local

import com.example.notificationvault.domain.model.ArchivedNotification

/**
 * Maps between Room database [NotificationEntity] and domain model [ArchivedNotification].
 */
fun NotificationEntity.toDomain(): ArchivedNotification {
    return ArchivedNotification(
        id = this.id,
        packageName = this.packageName,
        appLabel = this.appLabel,
        title = this.title,
        body = this.body,
        eventTimeMs = this.eventTimeMs,
        capturedAtMs = this.capturedAtMs,
        isBookmarked = this.isBookmarked,
        isRead = this.isRead,
        category = this.category,
        expiresAtMs = this.expiresAtMs,
        dedupeFingerprint = this.dedupeFingerprint,
        sourceNotificationKey = this.sourceNotificationKey
    )
}

fun ArchivedNotification.toEntity(): NotificationEntity {
    return NotificationEntity(
        id = this.id,
        packageName = this.packageName,
        appLabel = this.appLabel,
        title = this.title,
        body = this.body,
        eventTimeMs = this.eventTimeMs,
        capturedAtMs = this.capturedAtMs,
        isBookmarked = this.isBookmarked,
        isRead = this.isRead,
        category = this.category,
        expiresAtMs = this.expiresAtMs,
        dedupeFingerprint = this.dedupeFingerprint,
        sourceNotificationKey = this.sourceNotificationKey
    )
}

fun List<NotificationEntity>.toDomain(): List<ArchivedNotification> {
    return this.map { it.toDomain() }
}
