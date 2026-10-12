package com.example.notificationvault.ui.lifecycle

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.core.app.ApplicationProvider
import com.example.notificationvault.data.local.VaultDatabase
import com.example.notificationvault.data.repository.CaptureSettingsRepositoryImpl
import com.example.notificationvault.data.repository.NotificationRepositoryImpl
import com.example.notificationvault.data.repository.RetentionSettingsRepositoryImpl
import com.example.notificationvault.domain.model.ArchivedNotification
import com.example.notificationvault.platform.NotificationAccessChecker
import com.example.notificationvault.ui.onboarding.OnboardingViewModel
import com.example.notificationvault.ui.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PermissionLifecycleTransitionTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var database: VaultDatabase
    private lateinit var notificationRepository: NotificationRepositoryImpl
    private lateinit var captureSettingsRepository: CaptureSettingsRepositoryImpl
    private lateinit var retentionSettingsRepository: RetentionSettingsRepositoryImpl
    private lateinit var testAccessChecker: TestableAccessChecker

    private lateinit var onboardingViewModel: OnboardingViewModel
    private lateinit var settingsViewModel: SettingsViewModel

    class TestableAccessChecker(context: Context) : NotificationAccessChecker(context) {
        var isGranted: Boolean = true
        override fun isNotificationAccessGranted(): Boolean = isGranted
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        database = VaultDatabase.createInMemoryDatabase(context)
        notificationRepository = NotificationRepositoryImpl(database.notificationDao(), testDispatcher)
        captureSettingsRepository = CaptureSettingsRepositoryImpl(context, testDispatcher)
        retentionSettingsRepository = RetentionSettingsRepositoryImpl(context, testDispatcher)

        testAccessChecker = TestableAccessChecker(context).apply { isGranted = true }

        onboardingViewModel = OnboardingViewModel(testAccessChecker)
        settingsViewModel = SettingsViewModel(
            notificationRepository = notificationRepository,
            captureSettingsRepository = captureSettingsRepository,
            retentionSettingsRepository = retentionSettingsRepository,
            accessChecker = testAccessChecker
        )
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun onResume_reflectsPermissionRevocationAndRegrantingAcrossBothViewModels() = runTest(testDispatcher) {
        // Pre-save historical notification before revocation
        val historicalItem = ArchivedNotification(
            id = "notif_historical",
            packageName = "com.chat.app",
            title = "Existing Message",
            body = "Hello",
            eventTimeMs = 1000L,
            capturedAtMs = 1000L
        )
        notificationRepository.saveNotification(historicalItem)

        // Setup LifecycleOwner with LifecycleRegistry mimicking NotificationVaultApp's LifecycleEventObserver
        class TestOwner : LifecycleOwner {
            val registry = LifecycleRegistry(this)
            override val lifecycle: Lifecycle get() = registry
        }
        val owner = TestOwner()
        owner.registry.currentState = Lifecycle.State.RESUMED

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onboardingViewModel.refreshPermissionStatus()
                settingsViewModel.refreshPermissionStatus()
            }
        }
        owner.registry.addObserver(observer)

        // 1. Initial State: Access is granted
        val initialOnboarding = onboardingViewModel.uiState.first { it.isAccessGranted }
        val initialSettings = settingsViewModel.uiState.first { it.isAccessGranted }
        assertTrue("Onboarding must report granted initially", initialOnboarding.isAccessGranted)
        assertTrue("Settings must report granted initially", initialSettings.isAccessGranted)
        assertTrue("Global capture toggle is true by default", initialSettings.isGlobalCaptureEnabled)

        // 2. User backgrounded the app, opened Android system settings, and revoked access
        testAccessChecker.isGranted = false
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)

        // 3. User returns to app: triggers ON_RESUME
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        // Assert: Both ViewModels reflect revoked permission
        val revokedOnboarding = onboardingViewModel.uiState.first { !it.isAccessGranted }
        val revokedSettings = settingsViewModel.uiState.first { !it.isAccessGranted }
        assertFalse("Onboarding must reflect access revoked", revokedOnboarding.isAccessGranted)
        assertFalse("Settings must reflect access revoked", revokedSettings.isAccessGranted)

        // Crucial distinction: Keepbell's internal global capture toggle is distinct from system permission
        assertTrue("Global capture toggle preference remains unchanged", revokedSettings.isGlobalCaptureEnabled)

        // Revoking permission must NOT delete or mutate existing archive records
        val archiveCountAfterRevoke = notificationRepository.getNotificationCount().first()
        assertEquals(1, archiveCountAfterRevoke)
        assertEquals(historicalItem, notificationRepository.getNotificationByIdOnce("notif_historical"))

        // 4. User navigates back to system settings, re-enables access, and returns to app
        testAccessChecker.isGranted = true
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        // Assert: Both ViewModels reflect re-granted permission
        val reGrantedOnboarding = onboardingViewModel.uiState.first { it.isAccessGranted }
        val reGrantedSettings = settingsViewModel.uiState.first { it.isAccessGranted }
        assertTrue("Onboarding must reflect re-granted access", reGrantedOnboarding.isAccessGranted)
        assertTrue("Settings must reflect re-granted access", reGrantedSettings.isAccessGranted)

        owner.registry.removeObserver(observer)
    }
}
