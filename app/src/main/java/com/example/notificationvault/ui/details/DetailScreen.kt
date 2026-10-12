package com.example.notificationvault.ui.details

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.notificationvault.R
import com.example.notificationvault.domain.model.ArchivedNotification
import com.example.notificationvault.ui.common.AppIcon
import com.example.notificationvault.ui.common.EmptyStateView
import com.example.notificationvault.ui.common.LoadingStateView
import com.example.notificationvault.ui.common.rememberAppMetadata
import com.example.notificationvault.ui.theme.BookmarkGold
import com.example.notificationvault.util.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Saved Notification",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to inbox"
                        )
                    }
                },
                actions = {
                    if (uiState is DetailUiState.Success) {
                        val notification = (uiState as DetailUiState.Success).notification
                        IconButton(
                            onClick = { viewModel.toggleBookmark(notification.isBookmarked) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                painter = if (notification.isBookmarked) {
                                    painterResource(R.drawable.ic_star_filled)
                                } else {
                                    painterResource(R.drawable.ic_star_outline)
                                },
                                contentDescription = if (notification.isBookmarked) {
                                    "Remove bookmark"
                                } else {
                                    "Bookmark notification"
                                },
                                tint = if (notification.isBookmarked) {
                                    BookmarkGold
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete notification",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is DetailUiState.Loading -> {
                    LoadingStateView(modifier = Modifier.align(Alignment.Center))
                }

                is DetailUiState.NotFound -> {
                    EmptyStateView(
                        title = "Notification not found",
                        description = "This record may have been deleted or expired from your archive.",
                        icon = Icons.Default.Info,
                        actionLabel = "Back to Inbox",
                        onActionClick = onBackClick,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is DetailUiState.Success -> {
                    DetailContent(
                        notification = state.notification,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Delete notification?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This snapshot will be permanently removed from your local archive. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteNotification(onDeleted = onBackClick)
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailContent(
    notification: ArchivedNotification,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appMetadata by rememberAppMetadata(
        packageName = notification.packageName,
        initialLabel = notification.appLabel
    )
    val launchIntent = remember(notification.packageName) {
        context.packageManager.getLaunchIntentForPackage(notification.packageName)
    }

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Source App Identity Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon(
                        icon = appMetadata.icon,
                        label = appMetadata.label,
                        size = 44.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = appMetadata.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = notification.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (launchIntent != null) {
                    FilledTonalButton(
                        onClick = {
                            try {
                                context.startActivity(launchIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Unable to open application", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Open app",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Captured Notification Content Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Captured Text",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (!notification.title.isNullOrBlank()) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (!notification.body.isNullOrBlank()) {
                    Text(
                        text = notification.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                    )
                } else if (notification.title.isNullOrBlank()) {
                    Text(
                        text = "No title or message body was provided in this alert snapshot.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                } else {
                    Text(
                        text = "No message body was provided in this alert snapshot.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Snapshot Metadata Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Snapshot Metadata",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                MetadataRow(
                    label = "Event time",
                    value = DateTimeFormatter.format(notification.eventTimeMs)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                MetadataRow(
                    label = "Captured at",
                    value = DateTimeFormatter.format(notification.capturedAtMs)
                )

                if (!notification.category.isNullOrBlank()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                    MetadataRow(
                        label = "Category",
                        value = notification.category
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
