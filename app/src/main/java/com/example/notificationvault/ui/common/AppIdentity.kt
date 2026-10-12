package com.example.notificationvault.ui.common

import android.content.Context
import android.content.pm.PackageManager
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Resolved display identity of the app that posted a notification. */
data class AppIdentity(val label: String, val icon: ImageBitmap?)

/**
 * Resolves app label + icon once per package, off the main thread, and caches the result,
 * so nothing is looked up during recomposition.
 *
 * NOTE (Android 11+ package visibility): this only resolves apps the system lets you see.
 * Added a <queries> block to AndroidManifest.xml.
 */
class AppIdentityResolver private constructor(private val context: Context) {

    private val cache = LruCache<String, AppIdentity>(128)

    fun peek(packageName: String): AppIdentity? = cache.get(packageName)

    suspend fun resolve(packageName: String): AppIdentity {
        cache.get(packageName)?.let { return it }
        val identity = withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val info = pm.getApplicationInfo(packageName, 0)
                AppIdentity(
                    label = pm.getApplicationLabel(info).toString(),
                    icon = pm.getApplicationIcon(info).toBitmap(96, 96).asImageBitmap(),
                )
            } catch (e: PackageManager.NameNotFoundException) {
                AppIdentity(packageName, null)
            } catch (e: RuntimeException) {
                AppIdentity(packageName, null)
            }
        }
        cache.put(packageName, identity)
        return identity
    }

    companion object {
        @Volatile private var instance: AppIdentityResolver? = null

        fun get(context: Context): AppIdentityResolver =
            instance ?: synchronized(this) {
                instance ?: AppIdentityResolver(context.applicationContext).also { instance = it }
            }
    }
}

/**
 * Returns the best available identity immediately (cache, then stored label, then package name)
 * and upgrades it once the background lookup finishes.
 *
 * @param storedLabel the app label already saved with the notification, if your model has one.
 */
@Composable
fun rememberAppIdentity(packageName: String, storedLabel: String? = null): AppIdentity {
    val context = LocalContext.current
    val resolver = remember(context) { AppIdentityResolver.get(context) }
    val initialIdentity = remember(packageName, storedLabel) {
        resolver.peek(packageName)
            ?: AppIdentity(storedLabel?.takeIf { it.isNotBlank() } ?: packageName, null)
    }

    val state = remember(packageName, storedLabel) {
        mutableStateOf(initialIdentity)
    }

    LaunchedEffect(packageName, storedLabel) {
        val resolved = resolver.resolve(packageName)
        state.value = if (resolved.icon == null && resolved.label == packageName && !storedLabel.isNullOrBlank()) {
            resolved.copy(label = storedLabel)
        } else {
            resolved
        }
    }

    return state.value
}

