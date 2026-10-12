package com.example.notificationvault.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Localized date and time formatting utilities for notifications.
 */
object DateTimeFormatter {

    /**
     * Compact timestamp format for notification cards and summary lists.
     * - Today: "2:46 PM"
     * - Yesterday: "Yesterday"
     * - Same year: "Oct 12"
     * - Older: "10/12/25"
     */
    fun formatCompact(epochMs: Long): String {
        if (epochMs <= 0L) return ""
        return try {
            val eventCal = Calendar.getInstance().apply { timeInMillis = epochMs }
            val nowCal = Calendar.getInstance()

            val isToday = eventCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                eventCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

            if (isToday) {
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                return timeFormat.format(Date(epochMs))
            }

            val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val isYesterday = eventCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                eventCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)

            if (isYesterday) {
                return "Yesterday"
            }

            val isSameYear = eventCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)
            if (isSameYear) {
                val monthDayFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                return monthDayFormat.format(Date(epochMs))
            }

            val fullDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            fullDateFormat.format(Date(epochMs))
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Full date and time format for detail inspection screens.
     * Example: "Oct 12, 2026, 2:46 PM"
     */
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
