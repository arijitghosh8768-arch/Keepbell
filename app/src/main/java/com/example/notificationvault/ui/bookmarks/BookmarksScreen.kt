package com.example.notificationvault.ui.bookmarks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.notificationvault.ui.common.EmptyBookmarksState
import com.example.notificationvault.ui.common.LoadingState
import com.example.notificationvault.ui.common.NotificationCard
import com.example.notificationvault.ui.common.rememberAppIdentity
import com.example.notificationvault.ui.theme.Spacing
import com.example.notificationvault.util.formatCompactTime

@Composable
fun BookmarksScreen(
    viewModel: BookmarksViewModel,
    onNotificationClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val deleteCandidate by viewModel.deleteCandidate.collectAsState()

    // Confirmation dialog before deleting a bookmarked notification
    deleteCandidate?.let { candidate ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text("Delete notification?") },
            text = { Text("This will permanently remove this notification from your vault.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDelete) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg)
    ) {
        Spacer(modifier = Modifier.height(Spacing.sm))

        when (val state = uiState) {
            is BookmarksUiState.Loading -> {
                LoadingState(modifier = Modifier.weight(1f))
            }

            is BookmarksUiState.Empty -> {
                EmptyBookmarksState(modifier = Modifier.weight(1f))
            }

            is BookmarksUiState.Success -> {
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
