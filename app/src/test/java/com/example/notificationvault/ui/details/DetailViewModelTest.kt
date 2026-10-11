package com.example.notificationvault.ui.details

import com.example.notificationvault.data.local.VaultDatabase
import com.example.notificationvault.data.repository.NotificationRepositoryImpl
import com.example.notificationvault.domain.model.ArchivedNotification
import com.example.notificationvault.domain.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: VaultDatabase
    private lateinit var repository: NotificationRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = RuntimeEnvironment.getApplication()
        database = VaultDatabase.createInMemoryDatabase(context)
        repository = NotificationRepositoryImpl(database.notificationDao(), testDispatcher)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_whenNotificationDoesNotExist_emitsNotFound() = runTest(testDispatcher) {
        val viewModel = DetailViewModel(notificationId = "non_existent_id", repository = repository)
        val state = viewModel.uiState.first { it !is DetailUiState.Loading }
        assertTrue(state is DetailUiState.NotFound)
    }

    @Test
    fun uiState_whenNotificationExists_emitsSuccessAndMarksAsReadSafely() = runTest(testDispatcher) {
        val item = ArchivedNotification(
            id = "test_item_id",
            packageName = "com.sample.email",
            title = "New Email",
            body = "Hello there",
            eventTimeMs = 1000L,
            capturedAtMs = 1000L,
            isRead = false
        )
        repository.saveNotification(item)

        val viewModel = DetailViewModel(notificationId = "test_item_id", repository = repository)
        val state = viewModel.uiState.first { it !is DetailUiState.Loading }
        assertTrue(state is DetailUiState.Success)
        val loaded = (state as DetailUiState.Success).notification
        assertEquals("test_item_id", loaded.id)
        assertEquals("New Email", loaded.title)

        advanceUntilIdle()

        // Verify marked as read in database
        val fromDb = repository.getNotificationByIdOnce("test_item_id")
        assertTrue(fromDb?.isRead == true)
    }

    @Test
    fun toggleBookmark_updatesBookmarkStatus() = runTest(testDispatcher) {
        val item = ArchivedNotification(
            id = "test_item_id",
            packageName = "com.sample.email",
            title = "New Email",
            body = "Hello there",
            eventTimeMs = 1000L,
            capturedAtMs = 1000L,
            isBookmarked = false
        )
        repository.saveNotification(item)

        val viewModel = DetailViewModel(notificationId = "test_item_id", repository = repository)
        val state = viewModel.uiState.first { it !is DetailUiState.Loading }
        assertTrue(state is DetailUiState.Success)

        val job = viewModel.toggleBookmark(currentBookmark = false)
        job.join()

        val fromDb = repository.getNotificationByIdOnce("test_item_id")
        assertTrue(fromDb?.isBookmarked == true)
    }

    @Test
    fun deleteNotification_deletesRecordAndCallsCallback() = runTest(testDispatcher) {
        val item = ArchivedNotification(
            id = "test_item_id",
            packageName = "com.sample.email",
            title = "New Email",
            body = "Hello there",
            eventTimeMs = 1000L,
            capturedAtMs = 1000L
        )
        repository.saveNotification(item)

        val viewModel = DetailViewModel(notificationId = "test_item_id", repository = repository)
        val state = viewModel.uiState.first { it !is DetailUiState.Loading }
        assertTrue(state is DetailUiState.Success)

        var callbackCalled = false
        val job = viewModel.deleteNotification(onDeleted = {
            callbackCalled = true
        })
        job.join()

        assertTrue(callbackCalled)
        val fromDb = repository.getNotificationByIdOnce("test_item_id")
        assertNull(fromDb)
    }
}
