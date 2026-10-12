package com.example.notificationvault.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.notificationvault.domain.policy.RetentionPolicy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RetentionSettingsRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var repository: RetentionSettingsRepositoryImpl

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences(RetentionSettingsRepositoryImpl.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        repository = RetentionSettingsRepositoryImpl(context, testDispatcher)
    }

    @Test
    fun getRetentionPolicy_whenUnset_returnsDefault30Days() = runTest(testDispatcher) {
        val policy = repository.getRetentionPolicy()
        assertEquals(RetentionPolicy.DEFAULT, policy)
        assertEquals(RetentionPolicy.DAYS_30, policy)
    }

    @Test
    fun setRetentionPolicy_persistsAndUpdatesFlow() = runTest(testDispatcher) {
        repository.setRetentionPolicy(RetentionPolicy.DAYS_7)
        assertEquals(RetentionPolicy.DAYS_7, repository.getRetentionPolicy())
        assertEquals(RetentionPolicy.DAYS_7, repository.observeRetentionPolicy().first())

        repository.setRetentionPolicy(RetentionPolicy.KEEP_UNTIL_DELETED)
        assertEquals(RetentionPolicy.KEEP_UNTIL_DELETED, repository.getRetentionPolicy())
        assertEquals(RetentionPolicy.KEEP_UNTIL_DELETED, repository.observeRetentionPolicy().first())

        repository.setRetentionPolicy(RetentionPolicy.DAYS_90)
        assertEquals(RetentionPolicy.DAYS_90, repository.getRetentionPolicy())
    }

    @Test
    fun getRetentionPolicy_whenCorruptValueStored_fallsBackToDefault30Days() = runTest(testDispatcher) {
        val prefs = context.getSharedPreferences(RetentionSettingsRepositoryImpl.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(RetentionSettingsRepositoryImpl.KEY_RETENTION_DAYS, 12345).commit()

        val policy = repository.getRetentionPolicy()
        assertEquals(RetentionPolicy.DEFAULT, policy)
    }
}
