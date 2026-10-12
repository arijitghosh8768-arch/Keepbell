package com.example.notificationvault.app

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.material.icons.filled.Settings
import com.example.notificationvault.R
import com.example.notificationvault.data.local.VaultDatabase
import com.example.notificationvault.data.repository.CaptureSettingsRepositoryImpl
import com.example.notificationvault.data.repository.NotificationRepositoryImpl
import com.example.notificationvault.data.repository.RetentionSettingsRepositoryImpl
import com.example.notificationvault.platform.NotificationAccessChecker
import com.example.notificationvault.ui.bookmarks.BookmarksScreen
import com.example.notificationvault.ui.bookmarks.BookmarksViewModel
import com.example.notificationvault.ui.common.InboxTopBar
import com.example.notificationvault.ui.details.DetailScreen
import com.example.notificationvault.ui.details.DetailViewModel
import com.example.notificationvault.ui.inbox.InboxScreen
import com.example.notificationvault.ui.inbox.InboxViewModel
import com.example.notificationvault.ui.onboarding.OnboardingScreen
import com.example.notificationvault.ui.onboarding.OnboardingViewModel
import com.example.notificationvault.ui.settings.SettingsScreen
import com.example.notificationvault.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationVaultApp() {
    val context = LocalContext.current
    val accessChecker = remember(context) { NotificationAccessChecker(context) }
    val repository = remember(context) {
        NotificationRepositoryImpl(VaultDatabase.getInstance(context).notificationDao())
    }
    val captureSettingsRepository = remember(context) {
        CaptureSettingsRepositoryImpl(context)
    }
    val retentionSettingsRepository = remember(context) {
        RetentionSettingsRepositoryImpl(context)
    }

    val onboardingViewModel: OnboardingViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return OnboardingViewModel(accessChecker) as T
            }
        }
    )

    val settingsViewModel: SettingsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(
                    notificationRepository = repository,
                    captureSettingsRepository = captureSettingsRepository,
                    retentionSettingsRepository = retentionSettingsRepository,
                    accessChecker = accessChecker
                ) as T
            }
        }
    )

    val uiState by onboardingViewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Recheck permission state whenever app enters the foreground/resumes (ARCHITECTURE.md §6)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onboardingViewModel.refreshPermissionStatus()
                settingsViewModel.refreshPermissionStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val openSettings: () -> Unit = {
        try {
            context.startActivity(accessChecker.createSettingsIntent())
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "Unable to open settings. Please enable Notification Access manually in system settings.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    if (!uiState.isAccessGranted && !uiState.isDismissed) {
        OnboardingScreen(
            onOpenSettingsClick = openSettings,
            onNotNowClick = { onboardingViewModel.dismiss() }
        )
    } else {
        val navController = rememberNavController()
        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = currentBackStackEntry?.destination?.route

        val isMainDestination = currentRoute == "inbox" || currentRoute == "bookmarks" || currentRoute == "settings"

        Scaffold(
            topBar = {
                when (currentRoute) {
                    "inbox" -> InboxTopBar(title = "Keepbell")
                    "bookmarks" -> InboxTopBar(title = "Bookmarks")
                    "settings" -> InboxTopBar(title = "Settings")
                }
            },
            bottomBar = {
                if (isMainDestination) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentRoute == "inbox",
                            onClick = {
                                if (currentRoute != "inbox") {
                                    navController.navigate("inbox") {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Inbox"
                                )
                            },
                            label = { Text("Inbox") }
                        )

                        NavigationBarItem(
                            selected = currentRoute == "bookmarks",
                            onClick = {
                                if (currentRoute != "bookmarks") {
                                    navController.navigate("bookmarks") {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(
                                        if (currentRoute == "bookmarks") R.drawable.ic_star_filled
                                        else R.drawable.ic_star_outline
                                    ),
                                    contentDescription = "Bookmarks"
                                )
                            },
                            label = { Text("Bookmarks") }
                        )

                        NavigationBarItem(
                            selected = currentRoute == "settings",
                            onClick = {
                                if (currentRoute != "settings") {
                                    navController.navigate("settings") {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings"
                                )
                            },
                            label = { Text("Settings") }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                NavHost(
                    navController = navController,
                    startDestination = "inbox"
                ) {
                    composable("inbox") {
                        val inboxViewModel: InboxViewModel = viewModel(
                            factory = object : ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                    return InboxViewModel(repository) as T
                                }
                            }
                        )

                        InboxScreen(
                            viewModel = inboxViewModel,
                            isAccessGranted = uiState.isAccessGranted,
                            onOpenSettingsClick = openSettings,
                            onNotificationClick = { notificationId ->
                                navController.navigate("detail/$notificationId")
                            }
                        )
                    }

                    composable("bookmarks") {
                        val bookmarksViewModel: BookmarksViewModel = viewModel(
                            factory = object : ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                    return BookmarksViewModel(repository) as T
                                }
                            }
                        )

                        BookmarksScreen(
                            viewModel = bookmarksViewModel,
                            onNotificationClick = { notificationId ->
                                navController.navigate("detail/$notificationId")
                            }
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onOpenSystemSettingsClick = openSettings
                        )
                    }

                    composable(
                        route = "detail/{notificationId}",
                        arguments = listOf(
                            navArgument("notificationId") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val notificationId = backStackEntry.arguments?.getString("notificationId").orEmpty()
                        val detailViewModel: DetailViewModel = viewModel(
                            key = notificationId,
                            factory = object : ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                    return DetailViewModel(notificationId, repository) as T
                                }
                            }
                        )

                        DetailScreen(
                            viewModel = detailViewModel,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
