package com.example.notificationvault.ui.common

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Cached metadata representing source application label and icon.
 */
data class AppMetadata(
    val label: String,
    val icon: Drawable? = null
)

/**
 * Thread-safe, memory-cached resolver for application labels and icons.
 * Resolves metadata off the main thread via Dispatchers.IO.
 */
object AppMetadataResolver {
    private val cache = ConcurrentHashMap<String, AppMetadata>()

    suspend fun resolve(context: Context, packageName: String): AppMetadata {
        val cached = cache[packageName]
        if (cached != null) return cached

        return withContext(Dispatchers.IO) {
            val pm = context.packageManager
            try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                val label = pm.getApplicationLabel(appInfo)?.toString()?.takeIf { it.isNotBlank() } ?: packageName
                val icon = try {
                    pm.getApplicationIcon(appInfo)
                } catch (e: Exception) {
                    null
                }
                AppMetadata(label = label, icon = icon).also {
                    cache[packageName] = it
                }
            } catch (e: PackageManager.NameNotFoundException) {
                AppMetadata(label = packageName, icon = null).also {
                    cache[packageName] = it
                }
            } catch (e: Exception) {
                AppMetadata(label = packageName, icon = null)
            }
        }
    }
}

/**
 * Composable helper to safely observe app metadata without blocking composition or main thread.
 */
@Composable
fun rememberAppMetadata(
    packageName: String,
    initialLabel: String? = null
): State<AppMetadata> {
    val context = LocalContext.current.applicationContext
    val state = remember(packageName) {
        mutableStateOf(AppMetadata(label = initialLabel?.takeIf { it.isNotBlank() } ?: packageName))
    }

    LaunchedEffect(packageName) {
        val resolved = AppMetadataResolver.resolve(context, packageName)
        state.value = resolved
    }

    return state
}

