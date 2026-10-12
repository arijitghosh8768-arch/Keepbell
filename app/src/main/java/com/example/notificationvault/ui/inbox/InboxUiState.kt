package com.example.notificationvault.ui.inbox

import com.example.notificationvault.domain.model.ArchivedNotification

/**
 * UI state for the Inbox notification stream with explicit distinction between
 * a truly empty archive and active search/filter queries that yield zero results.
 */
sealed interface InboxUiState {
    data object Loading : InboxUiState

    /** Archive itself has 0 saved records across the whole database. */
    data object Empty : InboxUiState

    /** Active search query or filters produced no matching records in a non-empty archive. */
    data class NoResults(
        val query: String = "",
        val selectedPackage: String? = null,
        val datePreset: DateFilterPreset = DateFilterPreset.ALL_TIME
    ) : InboxUiState

    /** Search/filters produced one or more matching records. */
    data class Success(
        val notifications: List<ArchivedNotification>,
        val query: String = "",
        val selectedPackage: String? = null,
        val datePreset: DateFilterPreset = DateFilterPreset.ALL_TIME,
        val availablePackages: List<String> = emptyList()
    ) : InboxUiState
}
