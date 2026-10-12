package com.example.notificationvault.ui.bookmarks

import com.example.notificationvault.domain.model.ArchivedNotification

/**
 * UI State for the Bookmarks screen.
 */
sealed interface BookmarksUiState {
    object Loading : BookmarksUiState

    object Empty : BookmarksUiState

    data class Success(
        val notifications: List<ArchivedNotification>
    ) : BookmarksUiState
}
