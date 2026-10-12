package com.example.notificationvault.domain.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RetentionPolicyTest {

    @Test
    fun defaultPolicy_is30Days() {
        assertEquals(RetentionPolicy.DAYS_30, RetentionPolicy.DEFAULT)
        assertEquals(30, RetentionPolicy.DEFAULT.days)
    }

    @Test
    fun calculateCutoffMs_days7_returnsAccurateCutoff() {
        val nowMs = 1_000_000_000_000L
        val expected = nowMs - (7L * 24L * 60L * 60L * 1000L)
        val actual = RetentionPolicy.DAYS_7.calculateCutoffMs(nowMs)
        assertEquals(expected, actual)
    }

    @Test
    fun calculateCutoffMs_days30_returnsAccurateCutoff() {
        val nowMs = 1_000_000_000_000L
        val expected = nowMs - (30L * 24L * 60L * 60L * 1000L)
        val actual = RetentionPolicy.DAYS_30.calculateCutoffMs(nowMs)
        assertEquals(expected, actual)
    }

    @Test
    fun calculateCutoffMs_days90_returnsAccurateCutoff() {
        val nowMs = 1_000_000_000_000L
        val expected = nowMs - (90L * 24L * 60L * 60L * 1000L)
        val actual = RetentionPolicy.DAYS_90.calculateCutoffMs(nowMs)
        assertEquals(expected, actual)
    }

    @Test
    fun calculateCutoffMs_keepUntilDeleted_returnsNull() {
        val nowMs = 1_000_000_000_000L
        val actual = RetentionPolicy.KEEP_UNTIL_DELETED.calculateCutoffMs(nowMs)
        assertNull("Keep until deleted must return null cutoff to disable automatic deletion", actual)
    }

    @Test
    fun fromDays_validDays_resolvesCorrectEnum() {
        assertEquals(RetentionPolicy.DAYS_7, RetentionPolicy.fromDays(7))
        assertEquals(RetentionPolicy.DAYS_30, RetentionPolicy.fromDays(30))
        assertEquals(RetentionPolicy.DAYS_90, RetentionPolicy.fromDays(90))
        assertEquals(RetentionPolicy.KEEP_UNTIL_DELETED, RetentionPolicy.fromDays(-1))
        assertEquals(RetentionPolicy.KEEP_UNTIL_DELETED, RetentionPolicy.fromDays(null))
    }

    @Test
    fun fromDays_invalidOrCorruptDays_safelyFallsBackToDefault30Days() {
        assertEquals(RetentionPolicy.DEFAULT, RetentionPolicy.fromDays(0))
        assertEquals(RetentionPolicy.DEFAULT, RetentionPolicy.fromDays(15))
        assertEquals(RetentionPolicy.DEFAULT, RetentionPolicy.fromDays(365))
        assertEquals(RetentionPolicy.DEFAULT, RetentionPolicy.fromDays(-99))
    }
}
