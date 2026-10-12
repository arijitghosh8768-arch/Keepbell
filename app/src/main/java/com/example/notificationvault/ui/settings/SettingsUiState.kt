package com.example.notificationvault.ui.settings

import com.example.notificationvault.domain.policy.RetentionPolicy

/**
 * UI State for the Settings & Privacy screen.
 */
data class SettingsUiState(
    val isAccessGranted: Boolean = false,
    val isGlobalCaptureEnabled: Boolean = true,
    val retentionPolicy: RetentionPolicy = RetentionPolicy.DEFAULT,
    val excludedPackages: Set<String> = emptySet(),
    val availablePackages: List<String> = emptyList(),
    val showClearAllDialog: Boolean = false,
    val totalCount: Int = 0,
    val isClearing: Boolean = false,
    val clearActionResultMessage: String? = null
)
