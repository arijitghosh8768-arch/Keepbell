package com.example.notificationvault.ui.details

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
 * ViewModel managing notification detail inspection, safe read-status marking,
 * bookmark toggling, and explicit deletion.
 */
class DetailViewModel(
    private val notificationId: String,
    private val repository: NotificationRepository
) : ViewModel() {

    private var hasMarkedAsRead = false

    init {
        markAsRead()
    }

    private fun markAsRead() {
        if (!hasMarkedAsRead) {
            hasMarkedAsRead = true
            viewModelScope.launch {
                try {
                    repository.setRead(notificationId, true)
                } catch (e: Exception) {
                    // Sanitized handling
                }
            }
        }
    }

    val uiState: StateFlow<DetailUiState> = repository.getNotificationById(notificationId)
        .map { notification ->
            if (notification == null) {
                DetailUiState.NotFound
            } else {
                DetailUiState.Success(notification)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DetailUiState.Loading
        )

    fun toggleBookmark(currentBookmark: Boolean): Job {
        return viewModelScope.launch {
            repository.setBookmark(notificationId, !currentBookmark)
        }
    }

    fun deleteNotification(onDeleted: () -> Unit): Job {
        return viewModelScope.launch {
            repository.deleteNotification(notificationId)
            onDeleted()
        }
    }
}
