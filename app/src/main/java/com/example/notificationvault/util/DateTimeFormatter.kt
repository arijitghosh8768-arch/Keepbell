package com.example.notificationvault.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Localized date and time formatting utilities for notifications.
 */
object DateTimeFormatter {

    fun format(epochMs: Long): String {
        if (epochMs <= 0L) return "Unknown date"
        return try {
            val format = SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.getDefault())
            format.format(Date(epochMs))
        } catch (e: Exception) {
            "Unknown date"
        }
    }
}
