package com.example.notificationvault.ui.common

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

/** Compact brand bar. Pair with Scaffold + enableEdgeToEdge(); Scaffold supplies the insets. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxTopBar(modifier: Modifier = Modifier) {
    TopAppBar(
        title = {
            Text(
                "Notification Vault",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
        modifier = modifier,
    )
}
