package com.example.notificationvault.ui.inbox

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Supported date filtering presets with exact midnight epoch millisecond boundaries.
 */
enum class DateFilterPreset(val label: String) {
    ALL_TIME("All time"),
    TODAY("Today"),
    LAST_7_DAYS("Last 7 days"),
    LAST_30_DAYS("Last 30 days");

    /**
     * Calculates the inclusive start timestamp (epoch ms) and optional end timestamp for this preset.
     */
    fun calculateBoundaries(
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): Pair<Long?, Long?> {
        val today = now.atZone(zone).toLocalDate()
        return when (this) {
            ALL_TIME -> null to null
            TODAY -> {
                val startOfToday = today.atStartOfDay(zone).toInstant().toEpochMilli()
                startOfToday to null
            }
            LAST_7_DAYS -> {
                val sevenDaysAgo = today.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()
                sevenDaysAgo to null
            }
            LAST_30_DAYS -> {
                val thirtyDaysAgo = today.minusDays(29).atStartOfDay(zone).toInstant().toEpochMilli()
                thirtyDaysAgo to null
            }
        }
    }
}
