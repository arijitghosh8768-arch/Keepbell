package com.example.notificationvault.domain.policy

import kotlinx.coroutines.flow.Flow

/**
 * Domain interface for reading and updating the user's notification retention policy.
 */
interface RetentionSettingsRepository {

    /**
     * Observes the active [RetentionPolicy].
     */
    fun observeRetentionPolicy(): Flow<RetentionPolicy>

    /**
     * Retrieves the active [RetentionPolicy] once.
     * Guaranteed to fall back safely to [RetentionPolicy.DEFAULT] if missing or corrupted.
     */
    suspend fun getRetentionPolicy(): RetentionPolicy

    /**
     * Updates the user's chosen [RetentionPolicy].
     */
    suspend fun setRetentionPolicy(policy: RetentionPolicy)
}
