package com.example.notificationvault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.notificationvault.app.NotificationVaultApp
import com.example.notificationvault.ui.theme.NotificationVaultTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.notificationvault.worker.RetentionWorkerScheduler.schedulePeriodicCleanup(this)
        enableEdgeToEdge()
        setContent {
            NotificationVaultTheme {
                NotificationVaultApp()
            }
        }
    }
}
