package com.example.notificationvault.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.notificationvault.domain.policy.RetentionPolicy
import com.example.notificationvault.domain.policy.RetentionSettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * SharedPreferences-backed implementation of [RetentionSettingsRepository].
 *
 * Safely persists the user's chosen retention policy locally, validating stored values
 * and falling back to the 30-day default upon missing or unrecognized settings.
 */
class RetentionSettingsRepositoryImpl(
    private val sharedPreferences: SharedPreferences,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : RetentionSettingsRepository {

    constructor(context: Context, ioDispatcher: CoroutineDispatcher = Dispatchers.IO) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
        ioDispatcher
    )

    override fun observeRetentionPolicy(): Flow<RetentionPolicy> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_RETENTION_DAYS || key == null) {
                trySend(getRetentionPolicySync())
            }
        }
        trySend(getRetentionPolicySync())
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.flowOn(ioDispatcher)

    override suspend fun getRetentionPolicy(): RetentionPolicy = withContext(ioDispatcher) {
        getRetentionPolicySync()
    }

    override suspend fun setRetentionPolicy(policy: RetentionPolicy) = withContext(ioDispatcher) {
        val storedValue = policy.days ?: VALUE_KEEP_UNTIL_DELETED
        sharedPreferences.edit().putInt(KEY_RETENTION_DAYS, storedValue).apply()
    }

    private fun getRetentionPolicySync(): RetentionPolicy {
        return try {
            if (!sharedPreferences.contains(KEY_RETENTION_DAYS)) {
                RetentionPolicy.DEFAULT
            } else {
                val rawDays = sharedPreferences.getInt(KEY_RETENTION_DAYS, RetentionPolicy.DEFAULT.days ?: 30)
                RetentionPolicy.fromDays(rawDays)
            }
        } catch (_: Exception) {
            RetentionPolicy.DEFAULT
        }
    }

    companion object {
        const val PREFS_NAME = "notification_vault_retention_prefs"
        const val KEY_RETENTION_DAYS = "key_retention_days"
        const val VALUE_KEEP_UNTIL_DELETED = -1
    }
}
