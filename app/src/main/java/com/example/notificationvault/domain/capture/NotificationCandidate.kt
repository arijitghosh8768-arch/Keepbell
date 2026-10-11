package com.example.notificationvault.domain.capture

/**
 * Encapsulates raw extracted fields from a platform notification before persistence.
 */
data class NotificationCandidate(
    val packageName: String,
    val appLabel: String? = null,
    val title: String? = null,
    val body: String? = null,
    val eventTimeMs: Long,
    val sourceNotificationKey: String? = null,
    val category: String? = null
)
