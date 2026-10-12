package com.example.notificationvault.ui.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.notificationvault.data.local.VaultDatabase
import com.example.notificationvault.data.repository.CaptureSettingsRepositoryImpl
import com.example.notificationvault.data.repository.NotificationRepositoryImpl
import com.example.notificationvault.data.repository.RetentionSettingsRepositoryImpl
import com.example.notificationvault.domain.model.ArchivedNotification
import com.example.notificationvault.domain.policy.RetentionPolicy
import com.example.notificationvault.platform.NotificationAccessChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var database: VaultDatabase
    private lateinit var notificationRepository: NotificationRepositoryImpl
    private lateinit var captureSettingsRepository: CaptureSettingsRepositoryImpl
    private lateinit var retentionSettingsRepository: RetentionSettingsRepositoryImpl
    private lateinit var fakeAccessChecker: FakeAccessChecker
    private lateinit var viewModel: SettingsViewModel

    class FakeAccessChecker(context: Context) : NotificationAccessChecker(context) {
        var granted: Boolean = false
        override fun isNotificationAccessGranted(): Boolean = granted
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        database = VaultDatabase.createInMemoryDatabase(context)
        notificationRepository = NotificationRepositoryImpl(database.notificationDao(), testDispatcher)

        val capturePrefs = context.getSharedPreferences(CaptureSettingsRepositoryImpl.PREFS_NAME, Context.MODE_PRIVATE)
        capturePrefs.edit().clear().commit()
        captureSettingsRepository = CaptureSettingsRepositoryImpl(context, testDispatcher)

        val retentionPrefs = context.getSharedPreferences(RetentionSettingsRepositoryImpl.PREFS_NAME, Context.MODE_PRIVATE)
        retentionPrefs.edit().clear().commit()
        retentionSettingsRepository = RetentionSettingsRepositoryImpl(context, testDispatcher)

        fakeAccessChecker = FakeAccessChecker(context)
        viewModel = SettingsViewModel(
            notificationRepository = notificationRepository,
            captureSettingsRepository = captureSettingsRepository,
            retentionSettingsRepository = retentionSettingsRepository,
            accessChecker = fakeAccessChecker
        )
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun permissionStatus_refreshesWhenRequested() = runTest(testDispatcher) {
        fakeAccessChecker.granted = false
        viewModel.refreshPermissionStatus()
        val state1 = viewModel.uiState.first { !it.isAccessGranted }
        assertFalse("Permission should be false", state1.isAccessGranted)

        // User enables permission in system settings and returns to app (onResume)
        fakeAccessChecker.granted = true
        viewModel.refreshPermissionStatus()
        val state2 = viewModel.uiState.first { it.isAccessGranted }
        assertTrue("Permission should reflect true after resume refresh", state2.isAccessGranted)
    }

    @Test
    fun setGlobalCaptureEnabled_persistsAndUpdatesState() = runTest(testDispatcher) {
        viewModel.setGlobalCaptureEnabled(false).join()
        val stateDisabled = viewModel.uiState.first { !it.isGlobalCaptureEnabled }
        assertFalse(stateDisabled.isGlobalCaptureEnabled)
        assertFalse(captureSettingsRepository.getGlobalCaptureEnabled())

        viewModel.setGlobalCaptureEnabled(true).join()
        val stateEnabled = viewModel.uiState.first { it.isGlobalCaptureEnabled }
        assertTrue(stateEnabled.isGlobalCaptureEnabled)
        assertTrue(captureSettingsRepository.getGlobalCaptureEnabled())
    }

    @Test
    fun setRetentionPolicy_persistsAndUpdatesState() = runTest(testDispatcher) {
        viewModel.setRetentionPolicy(RetentionPolicy.DAYS_7).join()
        val state7 = viewModel.uiState.first { it.retentionPolicy == RetentionPolicy.DAYS_7 }
        assertEquals(RetentionPolicy.DAYS_7, state7.retentionPolicy)
        assertEquals(RetentionPolicy.DAYS_7, retentionSettingsRepository.getRetentionPolicy())

        viewModel.setRetentionPolicy(RetentionPolicy.KEEP_UNTIL_DELETED).join()
        val stateKeep = viewModel.uiState.first { it.retentionPolicy == RetentionPolicy.KEEP_UNTIL_DELETED }
        assertEquals(RetentionPolicy.KEEP_UNTIL_DELETED, stateKeep.retentionPolicy)
        assertEquals(RetentionPolicy.KEEP_UNTIL_DELETED, retentionSettingsRepository.getRetentionPolicy())
    }

    @Test
    fun setPackageExcluded_updatesExclusionsWithoutDeletingExistingNotifications() = runTest(testDispatcher) {
        val item = ArchivedNotification(
            id = "notif_1",
            packageName = "com.test.target",
            title = "Existing Record",
            eventTimeMs = 1000L,
            capturedAtMs = 1000L
        )
        notificationRepository.saveNotification(item)

        viewModel.setPackageExcluded("com.test.target", true).join()
        val stateExcluded = viewModel.uiState.first { it.excludedPackages.contains("com.test.target") }
        assertTrue(stateExcluded.excludedPackages.contains("com.test.target"))
        assertTrue(captureSettingsRepository.isPackageExcluded("com.test.target"))

        // Historical records must not be deleted by exclusion toggle
        val existing = notificationRepository.getNotificationByIdOnce("notif_1")
        assertEquals(item, existing)
    }

    @Test
    fun clearAll_whenDismissed_preservesDatabaseRecords() = runTest(testDispatcher) {
        val item1 = ArchivedNotification(id = "1", packageName = "com.a", eventTimeMs = 1L, capturedAtMs = 1L, isBookmarked = true)
        val item2 = ArchivedNotification(id = "2", packageName = "com.b", eventTimeMs = 2L, capturedAtMs = 2L, isBookmarked = false)
        notificationRepository.saveNotification(item1)
        notificationRepository.saveNotification(item2)

        viewModel.requestClearAll()
        val dialogShownState = viewModel.uiState.first { it.showClearAllDialog }
        assertTrue(dialogShownState.showClearAllDialog)

        viewModel.dismissClearAll()
        val dialogDismissedState = viewModel.uiState.first { !it.showClearAllDialog }
        assertFalse(dialogDismissedState.showClearAllDialog)

        // Database remains unchanged
        assertEquals(2, notificationRepository.getNotificationCount().first())
    }

    @Test
    fun clearAll_whenConfirmed_permanentlyDeletesAllNotificationsIncludingBookmarks() = runTest(testDispatcher) {
        val bookmarked = ArchivedNotification(
            id = "star_1",
            packageName = "com.important.app",
            title = "Starred Alert",
            isBookmarked = true,
            eventTimeMs = 1000L,
            capturedAtMs = 1000L
        )
        val unbookmarked = ArchivedNotification(
            id = "regular_1",
            packageName = "com.chat.app",
            title = "Regular Alert",
            isBookmarked = false,
            eventTimeMs = 2000L,
            capturedAtMs = 2000L
        )
        notificationRepository.saveNotification(bookmarked)
        notificationRepository.saveNotification(unbookmarked)

        viewModel.requestClearAll()
        val dialogShownState = viewModel.uiState.first { it.showClearAllDialog }
        assertTrue(dialogShownState.showClearAllDialog)

        viewModel.confirmClearAll().join()
        val clearedState = viewModel.uiState.first { !it.showClearAllDialog && !it.isClearing && it.clearActionResultMessage != null }

        assertFalse(clearedState.showClearAllDialog)
        assertFalse(clearedState.isClearing)

        // Entire archive including bookmarks must be 0
        assertEquals(0, notificationRepository.getNotificationCount().first())
        assertNull(notificationRepository.getNotificationByIdOnce("star_1"))
        assertNull(notificationRepository.getNotificationByIdOnce("regular_1"))
        assertEquals("Cleared 2 notifications.", clearedState.clearActionResultMessage)
    }
}
