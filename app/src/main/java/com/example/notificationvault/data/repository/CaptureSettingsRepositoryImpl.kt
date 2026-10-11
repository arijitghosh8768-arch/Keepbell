package com.example.notificationvault.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.notificationvault.domain.capture.CaptureSettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * SharedPreferences-backed implementation of [CaptureSettingsRepository].
 *
 * Persists user capture controls locally across app and service restarts.
 */
class CaptureSettingsRepositoryImpl(
    private val sharedPreferences: SharedPreferences,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : CaptureSettingsRepository {

    constructor(context: Context, ioDispatcher: CoroutineDispatcher = Dispatchers.IO) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
        ioDispatcher
    )

    override fun isGlobalCaptureEnabled(): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_GLOBAL_CAPTURE || key == null) {
                trySend(getGlobalCaptureEnabledSync())
            }
        }
        trySend(getGlobalCaptureEnabledSync())
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.flowOn(ioDispatcher)

    override suspend fun getGlobalCaptureEnabled(): Boolean = withContext(ioDispatcher) {
        getGlobalCaptureEnabledSync()
    }

    override suspend fun setGlobalCaptureEnabled(enabled: Boolean) = withContext(ioDispatcher) {
        sharedPreferences.edit().putBoolean(KEY_GLOBAL_CAPTURE, enabled).apply()
    }

    override fun getExcludedPackages(): Flow<Set<String>> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_EXCLUDED_PACKAGES || key == null) {
                trySend(getExcludedPackagesSync())
            }
        }
        trySend(getExcludedPackagesSync())
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.flowOn(ioDispatcher)

    override suspend fun getExcludedPackagesOnce(): Set<String> = withContext(ioDispatcher) {
        getExcludedPackagesSync()
    }

    override suspend fun isPackageExcluded(packageName: String): Boolean = withContext(ioDispatcher) {
        getExcludedPackagesSync().contains(packageName)
    }

    override suspend fun setPackageExcluded(packageName: String, excluded: Boolean) = withContext(ioDispatcher) {
        val current = getExcludedPackagesSync().toMutableSet()
        val changed = if (excluded) {
            current.add(packageName)
        } else {
            current.remove(packageName)
        }
        if (changed) {
            sharedPreferences.edit().putStringSet(KEY_EXCLUDED_PACKAGES, current).apply()
        }
    }

    private fun getGlobalCaptureEnabledSync(): Boolean {
        return sharedPreferences.getBoolean(KEY_GLOBAL_CAPTURE, DEFAULT_GLOBAL_CAPTURE)
    }

    private fun getExcludedPackagesSync(): Set<String> {
        return sharedPreferences.getStringSet(KEY_EXCLUDED_PACKAGES, emptySet()) ?: emptySet()
    }

    companion object {
        const val PREFS_NAME = "notification_vault_capture_prefs"
        const val KEY_GLOBAL_CAPTURE = "key_global_capture_enabled"
        const val KEY_EXCLUDED_PACKAGES = "key_excluded_packages"
        const val DEFAULT_GLOBAL_CAPTURE = true
    }
}
