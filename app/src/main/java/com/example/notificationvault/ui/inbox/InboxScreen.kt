package com.example.notificationvault.ui.inbox

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.notificationvault.ui.common.CaptureStatusBanner
import com.example.notificationvault.ui.common.EmptyInboxState
import com.example.notificationvault.ui.common.LoadingState
import com.example.notificationvault.ui.common.NoResultsState
import com.example.notificationvault.ui.common.NotificationCard
import com.example.notificationvault.ui.common.rememberAppIdentity
import com.example.notificationvault.ui.theme.Spacing
import com.example.notificationvault.util.formatCompactTime

@Composable
fun InboxScreen(
    viewModel: InboxViewModel,
    isAccessGranted: Boolean,
    onOpenSettingsClick: () -> Unit,
    onNotificationClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedPackage by viewModel.selectedPackage.collectAsState()
    val selectedDatePreset by viewModel.selectedDatePreset.collectAsState()
    val availablePackages by viewModel.availablePackages.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg)
    ) {
        Spacer(modifier = Modifier.height(Spacing.sm))

        // Slim capture banner
        CaptureStatusBanner(
            accessEnabled = isAccessGranted,
            onOpenSettings = onOpenSettingsClick
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        // Search text field
        SearchInputField(
            query = searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        // Filter chips (Date preset & App filter)
        FilterChipsRow(
            selectedDatePreset = selectedDatePreset,
            onDatePresetSelect = viewModel::onDatePresetSelect,
            selectedPackage = selectedPackage,
            onPackageSelect = viewModel::onPackageFilterSelect,
            availablePackages = availablePackages,
            onClearAll = viewModel::clearFilters,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        when (val state = uiState) {
            is InboxUiState.Loading -> {
                LoadingState(modifier = Modifier.weight(1f))
            }

            is InboxUiState.Empty -> {
                EmptyInboxState(
                    accessEnabled = isAccessGranted,
                    onOpenSettings = onOpenSettingsClick,
                    modifier = Modifier.weight(1f)
                )
            }

            is InboxUiState.NoResults -> {
                NoResultsState(
                    onClearFilters = viewModel::clearFilters,
                    modifier = Modifier.weight(1f)
                )
            }

            is InboxUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    contentPadding = PaddingValues(bottom = Spacing.xl)
                ) {
                    items(
                        items = state.notifications,
                        key = { it.id }
                    ) { notification ->
                        val app = rememberAppIdentity(
                            packageName = notification.packageName,
                            storedLabel = notification.appLabel
                        )

                        NotificationCard(
                            appLabel = app.label,
                            appIcon = app.icon,
                            title = notification.title,
                            body = notification.body,
                            timeText = formatCompactTime(notification.capturedAtMs),
                            isUnread = !notification.isRead,
                            isBookmarked = notification.isBookmarked,
                            onClick = { onNotificationClick(notification.id) },
                            onToggleBookmark = {
                                viewModel.toggleBookmark(notification.id, notification.isBookmarked)
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchInputField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Search title, body, or app...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        )
    )
}

@Composable
private fun FilterChipsRow(
    selectedDatePreset: DateFilterPreset,
    onDatePresetSelect: (DateFilterPreset) -> Unit,
    selectedPackage: String?,
    onPackageSelect: (String?) -> Unit,
    availablePackages: List<String>,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dateMenuExpanded by remember { mutableStateOf(false) }
    var packageMenuExpanded by remember { mutableStateOf(false) }

    val hasActiveFilter = selectedDatePreset != DateFilterPreset.ALL_TIME || selectedPackage != null

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Date Filter Chip
        Box {
            FilterChip(
                selected = selectedDatePreset != DateFilterPreset.ALL_TIME,
                onClick = { dateMenuExpanded = true },
                label = { Text(selectedDatePreset.label) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = if (selectedDatePreset != DateFilterPreset.ALL_TIME) {
                    {
                        IconButton(
                            onClick = { onDatePresetSelect(DateFilterPreset.ALL_TIME) },
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear date filter")
                        }
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors()
            )

            DropdownMenu(
                expanded = dateMenuExpanded,
                onDismissRequest = { dateMenuExpanded = false }
            ) {
                DateFilterPreset.entries.forEach { preset ->
                    DropdownMenuItem(
                        text = { Text(preset.label) },
                        onClick = {
                            onDatePresetSelect(preset)
                            dateMenuExpanded = false
                        }
                    )
                }
            }
        }

        // App Filter Chip
        if (availablePackages.isNotEmpty()) {
            Box {
                val appLabel = if (selectedPackage != null) {
                    val app = rememberAppIdentity(packageName = selectedPackage)
                    app.label
                } else {
                    "All apps"
                }

                FilterChip(
                    selected = selectedPackage != null,
                    onClick = { packageMenuExpanded = true },
                    label = { Text(appLabel) },
                    trailingIcon = if (selectedPackage != null) {
                        {
                            IconButton(
                                onClick = { onPackageSelect(null) },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear app filter")
                            }
                        }
                    } else null
                )

                DropdownMenu(
                    expanded = packageMenuExpanded,
                    onDismissRequest = { packageMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All apps") },
                        onClick = {
                            onPackageSelect(null)
                            packageMenuExpanded = false
                        }
                    )
                    availablePackages.forEach { pkg ->
                        val identity = rememberAppIdentity(packageName = pkg)
                        DropdownMenuItem(
                            text = { Text(identity.label) },
                            onClick = {
                                onPackageSelect(pkg)
                                packageMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Clear all filters chip
        if (hasActiveFilter) {
            FilterChip(
                selected = false,
                onClick = onClearAll,
                label = { Text("Reset") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Reset filters",
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}
