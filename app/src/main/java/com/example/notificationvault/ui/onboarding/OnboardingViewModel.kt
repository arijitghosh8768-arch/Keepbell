package com.example.notificationvault.ui.onboarding

import androidx.lifecycle.ViewModel
import com.example.notificationvault.platform.NotificationAccessChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * UI state representing permission and onboarding status.
 */
data class OnboardingUiState(
    val isAccessGranted: Boolean = false,
    val isDismissed: Boolean = false
)

/**
 * ViewModel managing permission state and onboarding actions.
 *
 * Ensures system permission status is freshly checked on app resume
 * rather than assuming past session state (per ARCHITECTURE.md §6).
 */
class OnboardingViewModel(
    private val accessChecker: NotificationAccessChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            isAccessGranted = accessChecker.isNotificationAccessGranted()
        )
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /**
     * Refreshes the permission status against the system state.
     */
    fun refreshPermissionStatus() {
        val granted = accessChecker.isNotificationAccessGranted()
        _uiState.update { it.copy(isAccessGranted = granted) }
    }

    /**
     * Dismisses the onboarding view when the user selects "Not now".
     */
    fun dismiss() {
        _uiState.update { it.copy(isDismissed = true) }
    }
}
