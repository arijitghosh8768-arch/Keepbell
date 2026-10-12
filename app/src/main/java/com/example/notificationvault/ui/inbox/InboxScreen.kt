package com.example.notificationvault.ui.inbox

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.notificationvault.ui.common.CaptureStatusBanner
import com.example.notificationvault.ui.common.EmptyInboxState
import com.example.notificationvault.ui.common.LoadingState
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg)
    ) {
        Spacer(modifier = Modifier.height(Spacing.md))

        CaptureStatusBanner(
            accessEnabled = isAccessGranted,
            onOpenSettings = onOpenSettingsClick
        )

        Spacer(modifier = Modifier.height(Spacing.md))

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
