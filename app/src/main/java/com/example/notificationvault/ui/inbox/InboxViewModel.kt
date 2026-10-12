package com.example.notificationvault.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notificationvault.domain.repository.NotificationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing search, source application filters, date ranges,
 * and the reactive notification archive stream.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class InboxViewModel(
    private val repository: NotificationRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPackage = MutableStateFlow<String?>(null)
    val selectedPackage: StateFlow<String?> = _selectedPackage.asStateFlow()

    private val _selectedDatePreset = MutableStateFlow(DateFilterPreset.ALL_TIME)
    val selectedDatePreset: StateFlow<DateFilterPreset> = _selectedDatePreset.asStateFlow()

    // Distinct packages observed in archive for filter chip selection
    val availablePackages: StateFlow<List<String>> = repository.getDistinctPackages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Debounced query stream (300ms) to prevent excessive DB queries during typing
    private val debouncedQuery = _searchQuery
        .debounce { query -> if (query.isEmpty()) 0L else 300L }

    // Filter pipeline combining debounced query, package filter, and date preset
    private val filteredNotifications = combine(
        debouncedQuery,
        _selectedPackage,
        _selectedDatePreset
    ) { query, pkg, datePreset ->
        Triple(query, pkg, datePreset)
    }.flatMapLatest { (query, pkg, datePreset) ->
        val (startTimeMs, endTimeMs) = datePreset.calculateBoundaries()
        val sanitizedQuery = query.trim().takeIf { it.isNotEmpty() }
        repository.filterNotifications(
            packageName = pkg,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            query = sanitizedQuery
        )
    }

    private data class FilterState(
        val query: String,
        val selectedPackage: String?,
        val datePreset: DateFilterPreset
    )

    private val filterState = combine(
        debouncedQuery,
        _selectedPackage,
        _selectedDatePreset
    ) { query, pkg, preset ->
        FilterState(query, pkg, preset)
    }

    // Combine filtered notifications with total count, filter state, and available packages
    val uiState: StateFlow<InboxUiState> = combine(
        filteredNotifications,
        repository.getNotificationCount(),
        filterState,
        availablePackages
    ) { notifications, totalCount, filters, packages ->
        val hasActiveFilter = filters.query.isNotBlank() || filters.selectedPackage != null || filters.datePreset != DateFilterPreset.ALL_TIME

        if (totalCount == 0) {
            InboxUiState.Empty
        } else if (notifications.isEmpty()) {
            if (hasActiveFilter) {
                InboxUiState.NoResults(
                    query = filters.query,
                    selectedPackage = filters.selectedPackage,
                    datePreset = filters.datePreset
                )
            } else {
                InboxUiState.Empty
            }
        } else {
            InboxUiState.Success(
                notifications = notifications,
                query = filters.query,
                selectedPackage = filters.selectedPackage,
                datePreset = filters.datePreset,
                availablePackages = packages
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InboxUiState.Loading
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onPackageFilterSelect(packageName: String?) {
        _selectedPackage.value = packageName
    }

    fun onDatePresetSelect(preset: DateFilterPreset) {
        _selectedDatePreset.value = preset
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedPackage.value = null
        _selectedDatePreset.value = DateFilterPreset.ALL_TIME
    }

    fun toggleBookmark(notificationId: String, currentBookmark: Boolean): Job {
        return viewModelScope.launch {
            repository.setBookmark(notificationId, !currentBookmark)
        }
    }
}
