package com.example.notificationvault.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notificationvault.domain.repository.NotificationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing the chronological notification archive stream.
 */
class InboxViewModel(
    private val repository: NotificationRepository
) : ViewModel() {

    val uiState: StateFlow<InboxUiState> = repository.getNotifications()
        .map { notifications ->
            if (notifications.isEmpty()) {
                InboxUiState.Empty
            } else {
                InboxUiState.Success(notifications)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = InboxUiState.Loading
        )

    fun toggleBookmark(notificationId: String, currentBookmark: Boolean): Job {
        return viewModelScope.launch {
            repository.setBookmark(notificationId, !currentBookmark)
        }
    }
}
