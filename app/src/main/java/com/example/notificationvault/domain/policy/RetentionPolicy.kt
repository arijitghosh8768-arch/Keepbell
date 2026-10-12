package com.example.notificationvault.domain.policy

/**
 * Supported retention policies for archived notifications (PRD FR-10, Requirements 11-12).
 *
 * @property days Number of days notifications are retained, or null if kept indefinitely.
 */
enum class RetentionPolicy(val days: Int?) {
    DAYS_7(7),
    DAYS_30(30),
    DAYS_90(90),
    KEEP_UNTIL_DELETED(null);

    /**
     * Calculates the millisecond cutoff timestamp based on the provided current time [nowMs].
     *
     * Records with capturedAtMs strictly less than this cutoff (< cutoffMs) and not bookmarked
     * are eligible for cleanup.
     *
     * Returns null if [KEEP_UNTIL_DELETED] is selected, indicating that automatic cleanup
     * is disabled and no records should be deleted.
     */
    fun calculateCutoffMs(nowMs: Long): Long? {
        val policyDays = days ?: return null
        val durationMs = policyDays.toLong() * MILLIS_PER_DAY
        return nowMs - durationMs
    }

    companion object {
        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

        /**
         * The default retention policy mandated by the product specification (30 days).
         */
        val DEFAULT = DAYS_30

        /**
         * Resolves a [RetentionPolicy] from a stored integer representation.
         *
         * - 7 -> [DAYS_7]
         * - 30 -> [DAYS_30]
         * - 90 -> [DAYS_90]
         * - -1 or null -> [KEEP_UNTIL_DELETED]
         * - Any other unknown or corrupt value falls back safely to [DEFAULT] (30 days).
         */
        fun fromDays(days: Int?): RetentionPolicy {
            return when (days) {
                7 -> DAYS_7
                30 -> DAYS_30
                90 -> DAYS_90
                -1, null -> KEEP_UNTIL_DELETED
                else -> DEFAULT
            }
        }
    }
}
