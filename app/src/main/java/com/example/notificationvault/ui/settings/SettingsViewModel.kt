package com.example.notificationvault.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notificationvault.domain.capture.CaptureSettingsRepository
import com.example.notificationvault.domain.policy.RetentionPolicy
import com.example.notificationvault.domain.policy.RetentionSettingsRepository
import com.example.notificationvault.domain.repository.NotificationRepository
import com.example.notificationvault.platform.NotificationAccessChecker
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing notification access status, global capture toggles,
 * per-app exclusions, retention policy selection, and confirmed Clear All archive deletion.
 */
class SettingsViewModel(
    private val notificationRepository: NotificationRepository,
    private val captureSettingsRepository: CaptureSettingsRepository,
    private val retentionSettingsRepository: RetentionSettingsRepository,
    private val accessChecker: NotificationAccessChecker
) : ViewModel() {

    private val _isAccessGranted = MutableStateFlow(accessChecker.isNotificationAccessGranted())
    private val _showClearAllDialog = MutableStateFlow(false)
    private val _isClearing = MutableStateFlow(false)
    private val _clearActionResultMessage = MutableStateFlow<String?>(null)

    private data class CaptureConfig(
        val isAccessGranted: Boolean,
        val isGlobalCaptureEnabled: Boolean,
        val retentionPolicy: RetentionPolicy,
        val excludedPackages: Set<String>
    )

    private val captureConfigFlow = combine(
        _isAccessGranted,
        captureSettingsRepository.isGlobalCaptureEnabled(),
        retentionSettingsRepository.observeRetentionPolicy(),
        captureSettingsRepository.getExcludedPackages()
    ) { access, globalCapture, retention, excluded ->
        CaptureConfig(access, globalCapture, retention, excluded)
    }

    private data class DialogState(
        val showDialog: Boolean,
        val isClearing: Boolean,
        val message: String?
    )

    private val dialogStateFlow = combine(
        _showClearAllDialog,
        _isClearing,
        _clearActionResultMessage
    ) { showDialog, clearing, message ->
        DialogState(showDialog, clearing, message)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        captureConfigFlow,
        notificationRepository.getDistinctPackages(),
        notificationRepository.getNotificationCount(),
        dialogStateFlow
    ) { config, distinctPackages, count, dialogState ->
        SettingsUiState(
            isAccessGranted = config.isAccessGranted,
            isGlobalCaptureEnabled = config.isGlobalCaptureEnabled,
            retentionPolicy = config.retentionPolicy,
            excludedPackages = config.excludedPackages,
            availablePackages = distinctPackages,
            showClearAllDialog = dialogState.showDialog,
            totalCount = count,
            isClearing = dialogState.isClearing,
            clearActionResultMessage = dialogState.message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SettingsUiState(
            isAccessGranted = accessChecker.isNotificationAccessGranted()
        )
    )

    /**
     * Refreshes the system Notification Access permission status.
     * Must be called when the app enters foreground / onResume from system settings.
     */
    fun refreshPermissionStatus() {
        _isAccessGranted.value = accessChecker.isNotificationAccessGranted()
    }

    fun setGlobalCaptureEnabled(enabled: Boolean): Job {
        return viewModelScope.launch {
            captureSettingsRepository.setGlobalCaptureEnabled(enabled)
        }
    }

    fun setRetentionPolicy(policy: RetentionPolicy): Job {
        return viewModelScope.launch {
            retentionSettingsRepository.setRetentionPolicy(policy)
        }
    }

    fun setPackageExcluded(packageName: String, excluded: Boolean): Job {
        return viewModelScope.launch {
            captureSettingsRepository.setPackageExcluded(packageName, excluded)
        }
    }

    fun requestClearAll() {
        _clearActionResultMessage.value = null
        _showClearAllDialog.value = true
    }

    fun dismissClearAll() {
        _showClearAllDialog.value = false
    }

    fun confirmClearAll(): Job {
        return viewModelScope.launch {
            _showClearAllDialog.value = false
            _isClearing.value = true
            try {
                // Permanently clears all records, including bookmarks, as specified in US-11 and DATABASE.md §6
                val deletedCount = notificationRepository.deleteAllNotifications()
                _clearActionResultMessage.value = "Cleared $deletedCount notification${if (deletedCount == 1) "" else "s"}."
            } catch (e: Exception) {
                _clearActionResultMessage.value = "Failed to clear notifications. Please try again."
            } finally {
                _isClearing.value = false
            }
        }
    }

    fun dismissResultMessage() {
        _clearActionResultMessage.value = null
    }
}
