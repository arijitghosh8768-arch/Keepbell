package com.example.notificationvault.domain.capture

import kotlinx.coroutines.flow.Flow

/**
 * Domain interface for user capture preferences (global capture toggle and per-package exclusion).
 */
interface CaptureSettingsRepository {

    /**
     * Observes whether global notification capture is enabled.
     */
    fun isGlobalCaptureEnabled(): Flow<Boolean>

    /**
     * Retrieves the current global capture toggle state once.
     */
    suspend fun getGlobalCaptureEnabled(): Boolean

    /**
     * Updates the global capture toggle state.
     */
    suspend fun setGlobalCaptureEnabled(enabled: Boolean)

    /**
     * Observes the set of package names excluded from capture.
     */
    fun getExcludedPackages(): Flow<Set<String>>

    /**
     * Retrieves the set of package names excluded from capture once.
     */
    suspend fun getExcludedPackagesOnce(): Set<String>

    /**
     * Checks whether a specific package name is excluded from capture.
     */
    suspend fun isPackageExcluded(packageName: String): Boolean

    /**
     * Updates exclusion state for a specific package name.
     */
    suspend fun setPackageExcluded(packageName: String, excluded: Boolean)
}
