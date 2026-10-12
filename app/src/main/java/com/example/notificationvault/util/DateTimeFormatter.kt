package com.example.notificationvault.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Compact timestamp for list cards: time today, "Yesterday", "Oct 12", or "Oct 12, 2025".
 * Uses java.time (minSdk 26+).
 */
fun formatCompactTime(
    epochMillis: Long,
    yesterdayLabel: String = "Yesterday",
    now: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): String {
    if (epochMillis <= 0L) return ""
    return try {
        val time = Instant.ofEpochMilli(epochMillis).atZone(zone)
        val day = time.toLocalDate()
        val today = now.atZone(zone).toLocalDate()
        when {
            day == today -> DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(time)
            day == today.minusDays(1) -> yesterdayLabel
            day.year == today.year -> DateTimeFormatter.ofPattern("MMM d", locale).format(time)
            else -> DateTimeFormatter.ofPattern("MMM d, yyyy", locale).format(time)
        }
    } catch (e: Exception) {
        ""
    }
}

/** Full timestamp for the detail screen. */
fun formatFullTime(
    epochMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): String {
    if (epochMillis <= 0L) return "Unknown date"
    return try {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(locale)
            .format(Instant.ofEpochMilli(epochMillis).atZone(zone))
    } catch (e: Exception) {
        "Unknown date"
    }
}

/**
 * Backward compatibility facade for any callers relying on DateTimeFormatter object.
 */
object DateTimeFormatter {
    fun format(epochMs: Long): String = formatFullTime(epochMs)
    fun formatCompact(epochMs: Long): String = formatCompactTime(epochMs)
}
