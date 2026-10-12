package com.example.notificationvault.ui.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notificationvault.domain.model.ArchivedNotification
import com.example.notificationvault.domain.repository.NotificationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing the bookmarked notification stream, unbookmarking,
 * and deletion confirmation flow for the Bookmarks screen.
 */
class BookmarksViewModel(
    private val repository: NotificationRepository
) : ViewModel() {

    // Notification marked for deletion confirmation dialog; null if dialog is hidden
    private val _deleteCandidate = MutableStateFlow<ArchivedNotification?>(null)
    val deleteCandidate: StateFlow<ArchivedNotification?> = _deleteCandidate.asStateFlow()

    val uiState: StateFlow<BookmarksUiState> = repository.getBookmarkedNotifications()
        .map { notifications ->
            if (notifications.isEmpty()) {
                BookmarksUiState.Empty
            } else {
                BookmarksUiState.Success(notifications = notifications)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BookmarksUiState.Loading
        )

    fun toggleBookmark(notificationId: String, currentBookmark: Boolean): Job {
        return viewModelScope.launch {
            repository.setBookmark(notificationId, !currentBookmark)
        }
    }

    fun requestDelete(notification: ArchivedNotification) {
        _deleteCandidate.value = notification
    }

    fun dismissDelete() {
        _deleteCandidate.value = null
    }

    fun confirmDelete(): Job {
        val candidate = _deleteCandidate.value ?: return Job().apply { complete() }
        return viewModelScope.launch {
            repository.deleteNotification(candidate.id)
            _deleteCandidate.value = null
        }
    }
}
