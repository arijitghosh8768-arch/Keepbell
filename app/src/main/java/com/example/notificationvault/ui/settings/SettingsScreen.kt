package com.example.notificationvault.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.notificationvault.domain.policy.RetentionPolicy
import com.example.notificationvault.ui.common.AppAvatar
import com.example.notificationvault.ui.common.rememberAppIdentity
import com.example.notificationvault.ui.theme.Spacing

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenSystemSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.clearActionResultMessage) {
        uiState.clearActionResultMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissResultMessage()
        }
    }

    if (uiState.showClearAllDialog) {
        ClearAllConfirmationDialog(
            totalCount = uiState.totalCount,
            onConfirm = viewModel::confirmClearAll,
            onDismiss = viewModel::dismissClearAll
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            // 1. Notification Access (System permission)
            SettingsCard(title = "Notification Access") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.isAccessGranted) "Access granted" else "Access required",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (uiState.isAccessGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.isAccessGranted) {
                                "Keepbell has Android permission to observe incoming notifications."
                            } else {
                                "Keepbell cannot capture any notifications until permission is enabled in Android settings."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!uiState.isAccessGranted) {
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Button(onClick = onOpenSystemSettingsClick) {
                            Text("Enable")
                        }
                    }
                }
            }

            // 2. Capture Control
            SettingsCard(title = "Notification Capture") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Save incoming notifications",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.isGlobalCaptureEnabled) {
                                "New eligible notifications will be saved to your local archive."
                            } else {
                                "Capture is paused. Incoming notifications will not be saved."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = uiState.isGlobalCaptureEnabled,
                        onCheckedChange = { viewModel.setGlobalCaptureEnabled(it) }
                    )
                }
            }

            // 3. Retention Policy
            SettingsCard(title = "Auto-Cleanup Retention") {
                Text(
                    text = "Automatically deletes expired notifications on the daily cleanup run. Bookmarked notifications are never auto-deleted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                RetentionOptionRow(
                    title = "7 days",
                    selected = uiState.retentionPolicy == RetentionPolicy.DAYS_7,
                    onClick = { viewModel.setRetentionPolicy(RetentionPolicy.DAYS_7) }
                )
                RetentionOptionRow(
                    title = "30 days (Default)",
                    selected = uiState.retentionPolicy == RetentionPolicy.DAYS_30,
                    onClick = { viewModel.setRetentionPolicy(RetentionPolicy.DAYS_30) }
                )
                RetentionOptionRow(
                    title = "90 days",
                    selected = uiState.retentionPolicy == RetentionPolicy.DAYS_90,
                    onClick = { viewModel.setRetentionPolicy(RetentionPolicy.DAYS_90) }
                )
                RetentionOptionRow(
                    title = "Keep until deleted",
                    selected = uiState.retentionPolicy == RetentionPolicy.KEEP_UNTIL_DELETED,
                    onClick = { viewModel.setRetentionPolicy(RetentionPolicy.KEEP_UNTIL_DELETED) }
                )
            }

            // 4. Per-App Exclusions
            if (uiState.availablePackages.isNotEmpty()) {
                SettingsCard(title = "App Exclusions") {
                    Text(
                        text = "Turn off capture for specific apps. Disabling an app prevents future captures without deleting past notifications.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        uiState.availablePackages.forEach { pkg ->
                            val app = rememberAppIdentity(packageName = pkg, storedLabel = null)
                            val isExcluded = uiState.excludedPackages.contains(pkg)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppAvatar(icon = app.icon, size = 32.dp)
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (isExcluded) "Capture blocked" else "Capturing",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isExcluded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                                Switch(
                                    checked = !isExcluded,
                                    onCheckedChange = { capturing ->
                                        viewModel.setPackageExcluded(pkg, !capturing)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 5. Data & Privacy (Local-First Guarantee & Clear All)
            SettingsCard(title = "Data & Privacy") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = "100% Local-First & Private",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "All notifications and settings stay strictly on this device. Keepbell has no servers, no telemetry, no tracking, and no network access.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.md))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Clear Archive",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "${uiState.totalCount} saved notifications currently stored.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = viewModel::requestClearAll,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        enabled = !uiState.isClearing && uiState.totalCount > 0
                    ) {
                        if (uiState.isClearing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Clear all")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            content()
        }
    }
}

@Composable
private fun RetentionOptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(Spacing.xs))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun ClearAllConfirmationDialog(
    totalCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(text = "Clear entire archive?")
        },
        text = {
            Text(
                text = "This will permanently delete all $totalCount notifications, including any bookmarked items. This action cannot be undone."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Clear everything")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
