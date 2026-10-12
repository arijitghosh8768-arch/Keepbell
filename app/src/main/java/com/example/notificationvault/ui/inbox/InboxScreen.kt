package com.example.notificationvault.ui.inbox

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.notificationvault.R
import com.example.notificationvault.domain.model.ArchivedNotification
import com.example.notificationvault.ui.common.AppIcon
import com.example.notificationvault.ui.common.CaptureStatusBanner
import com.example.notificationvault.ui.common.EmptyStateView
import com.example.notificationvault.ui.common.LoadingStateView
import com.example.notificationvault.ui.common.rememberAppMetadata
import com.example.notificationvault.ui.theme.BookmarkGold
import com.example.notificationvault.ui.theme.UnreadDotColor
import com.example.notificationvault.util.DateTimeFormatter

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
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Slim, non-intrusive status banner
        CaptureStatusBanner(
            isAccessGranted = isAccessGranted,
            onOpenSettingsClick = onOpenSettingsClick
        )

        Spacer(modifier = Modifier.height(12.dp))

        when (val state = uiState) {
            is InboxUiState.Loading -> {
                LoadingStateView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }

            is InboxUiState.Empty -> {
                EmptyStateView(
                    title = "No saved notifications yet",
                    description = if (isAccessGranted) {
                        "Incoming notifications will appear here once captured on this device."
                    } else {
                        "Vault needs notification access in Android settings to begin saving alerts locally."
                    },
                    icon = Icons.Default.Notifications,
                    actionLabel = if (!isAccessGranted) "Open Settings" else null,
                    onActionClick = if (!isAccessGranted) onOpenSettingsClick else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }

            is InboxUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(
                        items = state.notifications,
                        key = { it.id }
                    ) { notification ->
                        NotificationCard(
                            notification = notification,
                            onClick = { onNotificationClick(notification.id) },
                            onBookmarkToggle = {
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
fun NotificationCard(
    notification: ArchivedNotification,
    onClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Resolve app label and icon off the main thread with coroutine memory cache
    val appMetadata by rememberAppMetadata(
        packageName = notification.packageName,
        initialLabel = notification.appLabel
    )

    // Card background: prominent container for unread, calm tonal container for read
    val containerColor = if (!notification.isRead) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = buildString {
                    append(if (!notification.isRead) "Unread notification, " else "Read notification, ")
                    append("from ${appMetadata.label}. ")
                    if (!notification.title.isNullOrBlank()) {
                        append("Title: ${notification.title}. ")
                    }
                    append(notification.displayBody)
                }
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (!notification.isRead) 1.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            // App icon with fallback monogram
            AppIcon(
                icon = appMetadata.icon,
                label = appMetadata.label,
                size = 36.dp,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Main body column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .animateContentSize()
            ) {
                // Header row: App name + unread badge + compact timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = appMetadata.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (!notification.isRead) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(UnreadDotColor)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = DateTimeFormatter.formatCompact(notification.capturedAtMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Notification Title
                if (!notification.title.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Body preview (2-3 lines max, ellipsis)
                if (!notification.body.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = notification.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (notification.title.isNullOrBlank()) {
                    // Both title and body missing - graceful compact placeholder
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Notification content not provided",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            // Bookmark button (minimum 48dp touch target)
            IconButton(
                onClick = onBookmarkToggle,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = if (notification.isBookmarked) {
                        painterResource(R.drawable.ic_star_filled)
                    } else {
                        painterResource(R.drawable.ic_star_outline)
                    },
                    contentDescription = if (notification.isBookmarked) {
                        "Remove bookmark for ${appMetadata.label}"
                    } else {
                        "Bookmark notification from ${appMetadata.label}"
                    },
                    tint = if (notification.isBookmarked) {
                        BookmarkGold
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
