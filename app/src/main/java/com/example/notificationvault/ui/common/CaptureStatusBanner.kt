package com.example.notificationvault.ui.common

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.notificationvault.ui.theme.Spacing

/** Opens the system screen where the user grants/revokes notification access. */
fun openNotificationAccessSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

/**
 * Slim capture status. [accessEnabled] must come from the real Android permission state
 * (e.g. NotificationManagerCompat.getEnabledListenerPackages), never a constant.
 */
@Composable
fun CaptureStatusBanner(
    accessEnabled: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val container = if (accessEnabled) colors.secondaryContainer else colors.errorContainer
    val content = if (accessEnabled) colors.onSecondaryContainer else colors.onErrorContainer

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (accessEnabled) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f).padding(start = Spacing.md)) {
                Text(
                    text = if (accessEnabled) "Capture is on" else "Capture is off",
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = if (accessEnabled) {
                        "New notifications are saved locally on this device."
                    } else {
                        "Notification access is disabled, so new notifications aren't being saved."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (!accessEnabled) {
                Spacer(Modifier.width(Spacing.sm))
                TextButton(onClick = onOpenSettings) { Text("Open settings") }
            }
        }
    }
}
