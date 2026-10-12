package com.example.notificationvault.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.notificationvault.ui.theme.Spacing

@Composable
private fun CenteredMessage(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    Box(modifier.fillMaxSize().padding(Spacing.xl), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(Spacing.lg))
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(Spacing.sm))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (action != null) {
                Spacer(Modifier.height(Spacing.lg))
                action()
            }
        }
    }
}

/** Empty inbox when the database has 0 saved records. */
@Composable
fun EmptyInboxState(accessEnabled: Boolean, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    if (accessEnabled) {
        CenteredMessage(
            icon = Icons.Filled.Notifications,
            title = "Nothing saved yet",
            message = "New notifications will appear here as they arrive. Notifications that arrived before capture was on can't be recovered.",
            modifier = modifier,
        )
    } else {
        CenteredMessage(
            icon = Icons.Filled.Notifications,
            title = "Capture is off",
            message = "Turn on notification access so new notifications can be saved to this device.",
            modifier = modifier,
            action = { Button(onClick = onOpenSettings) { Text("Open settings") } },
        )
    }
}

/** No results state when active filters or search query produce 0 matches. */
@Composable
fun NoResultsState(onClearFilters: () -> Unit, modifier: Modifier = Modifier) {
    CenteredMessage(
        icon = Icons.Filled.Search,
        title = "No matching notifications",
        message = "No saved notifications matched your search or filter criteria. Try searching for a different term or clearing filters.",
        modifier = modifier,
        action = { Button(onClick = onClearFilters) { Text("Clear filters") } }
    )
}

/** Empty bookmarks state when no notifications are bookmarked. */
@Composable
fun EmptyBookmarksState(modifier: Modifier = Modifier) {
    CenteredMessage(
        icon = Icons.Filled.Notifications,
        title = "No bookmarks yet",
        message = "Tap the star icon on any notification card in your Inbox to save it here for quick access.",
        modifier = modifier
    )
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxSize().semantics { contentDescription = "Loading" },
        contentAlignment = Alignment.Center,
    ) { CircularProgressIndicator() }
}

@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    CenteredMessage(
        icon = Icons.Filled.Warning,
        title = "Something went wrong",
        message = message,
        modifier = modifier,
        action = if (onRetry != null) {
            { OutlinedButton(onClick = onRetry) { Text("Try again") } }
        } else {
            null
        },
    )
}
