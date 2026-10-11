package com.example.notificationvault.domain.model

import java.util.UUID

/**
 * Domain representation of an archived notification snapshot.
 *
 * This immutable model decouples domain and UI layers from database entity specifics.
 */
data class ArchivedNotification(
    val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val appLabel: String? = null,
    val title: String? = null,
    val body: String? = null,
    val eventTimeMs: Long,
    val capturedAtMs: Long,
    val isBookmarked: Boolean = false,
    val isRead: Boolean = false,
    val category: String? = null,
    val expiresAtMs: Long? = null,
    val dedupeFingerprint: String? = null,
    val sourceNotificationKey: String? = null
) {
    /**
     * Display name for the source application with a safe fallback to package name.
     */
    val displayAppLabel: String
        get() = appLabel?.takeIf { it.isNotBlank() } ?: packageName

    /**
     * Preview text for UI display, showing a standard placeholder if body text is unavailable.
     */
    val displayBody: String
        get() = body?.takeIf { it.isNotBlank() } ?: "No preview available"
}
