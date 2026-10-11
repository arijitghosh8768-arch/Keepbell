package com.example.notificationvault.ui.inbox

import com.example.notificationvault.domain.model.ArchivedNotification

/**
 * UI state for the Inbox notification stream.
 */
sealed interface InboxUiState {
    data object Loading : InboxUiState
    data object Empty : InboxUiState
    data class Success(
        val notifications: List<ArchivedNotification>
    ) : InboxUiState
}
