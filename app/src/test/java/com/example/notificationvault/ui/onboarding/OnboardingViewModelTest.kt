package com.example.notificationvault.ui.onboarding

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.notificationvault.platform.NotificationAccessChecker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnboardingViewModelTest {

    private lateinit var context: Context
    private lateinit var accessChecker: NotificationAccessChecker
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        accessChecker = NotificationAccessChecker(context)
        viewModel = OnboardingViewModel(accessChecker)
    }

    @Test
    fun initialState_defaultsToAccessNotGrantedAndNotDismissed() {
        val state = viewModel.uiState.value

        assertFalse(state.isAccessGranted)
        assertFalse(state.isDismissed)
    }

    @Test
    fun dismiss_setsDismissedToTrue() {
        viewModel.dismiss()

        val state = viewModel.uiState.value
        assertTrue(state.isDismissed)
    }

    @Test
    fun refreshPermissionStatus_queriesAccessChecker() {
        // Initially false
        viewModel.refreshPermissionStatus()
        assertFalse(viewModel.uiState.value.isAccessGranted)
    }
}
