package com.example.notificationvault.data.repository

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CaptureSettingsRepositoryImplTest {

    private lateinit var repository: CaptureSettingsRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = context.getSharedPreferences("test_capture_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        repository = CaptureSettingsRepositoryImpl(prefs)
    }

    @Test
    fun defaultSettings_haveGlobalCaptureEnabledAndNoExcludedPackages() = runBlocking {
        assertTrue(repository.getGlobalCaptureEnabled())
        assertTrue(repository.isGlobalCaptureEnabled().first())
        assertTrue(repository.getExcludedPackagesOnce().isEmpty())
        assertTrue(repository.getExcludedPackages().first().isEmpty())
        assertFalse(repository.isPackageExcluded("com.any.app"))
    }

    @Test
    fun setGlobalCaptureEnabled_persistsAndUpdatesFlow() = runBlocking {
        repository.setGlobalCaptureEnabled(false)

        assertFalse(repository.getGlobalCaptureEnabled())
        assertFalse(repository.isGlobalCaptureEnabled().first())

        repository.setGlobalCaptureEnabled(true)
        assertTrue(repository.getGlobalCaptureEnabled())
        assertTrue(repository.isGlobalCaptureEnabled().first())
    }

    @Test
    fun setPackageExcluded_addsAndRemovesExclusions() = runBlocking {
        repository.setPackageExcluded("com.spam.promotions", true)

        assertTrue(repository.isPackageExcluded("com.spam.promotions"))
        assertEquals(setOf("com.spam.promotions"), repository.getExcludedPackagesOnce())
        assertEquals(setOf("com.spam.promotions"), repository.getExcludedPackages().first())

        repository.setPackageExcluded("com.another.app", true)
        assertEquals(setOf("com.spam.promotions", "com.another.app"), repository.getExcludedPackagesOnce())

        repository.setPackageExcluded("com.spam.promotions", false)
        assertFalse(repository.isPackageExcluded("com.spam.promotions"))
        assertTrue(repository.isPackageExcluded("com.another.app"))
        assertEquals(setOf("com.another.app"), repository.getExcludedPackagesOnce())
    }
}
