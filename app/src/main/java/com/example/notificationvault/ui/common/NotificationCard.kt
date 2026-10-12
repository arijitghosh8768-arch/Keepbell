package com.example.notificationvault.ui.common

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.notificationvault.R
import com.example.notificationvault.ui.theme.Spacing

/**
 * One archived notification. Pure UI: pass plain values from your existing UI model.
 * Truncation here is visual only; the full stored text is shown on the detail screen.
 */
@Composable
fun NotificationCard(
    appLabel: String,
    appIcon: ImageBitmap?,
    title: String?,
    body: String?,
    timeText: String,
    isUnread: Boolean,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val hasTitle = !title.isNullOrBlank()
    val hasBody = !body.isNullOrBlank()

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (isUnread) colors.primaryContainer.copy(alpha = 0.35f) else colors.surface,
        border = if (isUnread) null else BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(start = Spacing.lg, top = Spacing.md, bottom = Spacing.md, end = Spacing.xs),
            verticalAlignment = Alignment.Top,
        ) {
            AppAvatar(icon = appIcon)
            Spacer(Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = appLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isUnread) colors.primary else colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                    )
                    if (isUnread) {
                        Spacer(Modifier.width(Spacing.sm))
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors.primary)
                                .semantics { contentDescription = "Unread" },
                        )
                    }
                }

                if (hasTitle) {
                    Text(
                        text = title!!,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                        color = colors.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
                if (hasBody) {
                    Text(
                        text = body!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
                if (!hasTitle && !hasBody) {
                    Text(
                        text = "No preview available",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
            }

            BookmarkButton(isBookmarked = isBookmarked, onToggle = onToggleBookmark)
        }
    }
}

@Composable
fun AppAvatar(icon: ImageBitmap?, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val shape = RoundedCornerShape(12.dp)
    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = null, // decorative; the app name is read next to it
            modifier = modifier.size(size).clip(shape),
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(size / 2),
            )
        }
    }
}

/** 48dp bookmark toggle with a short crossfade and tint animation using local vector drawables. */
@Composable
fun BookmarkButton(isBookmarked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val tint by animateColorAsState(
        targetValue = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(150),
        label = "bookmarkTint",
    )
    IconButton(onClick = onToggle, modifier = modifier.size(48.dp)) {
        Crossfade(targetState = isBookmarked, animationSpec = tween(150), label = "bookmarkIcon") { saved ->
            Icon(
                painter = if (saved) painterResource(R.drawable.ic_star_filled) else painterResource(R.drawable.ic_star_outline),
                contentDescription = if (saved) "Remove bookmark" else "Bookmark",
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
