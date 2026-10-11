package com.example.notificationvault.ui.details

import com.example.notificationvault.domain.model.ArchivedNotification

/**
 * UI state for the Notification Detail view.
 */
sealed interface DetailUiState {
    data object Loading : DetailUiState
    data object NotFound : DetailUiState
    data class Success(
        val notification: ArchivedNotification
    ) : DetailUiState
}
