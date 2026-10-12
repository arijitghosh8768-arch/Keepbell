package com.example.notificationvault.ui.bookmarks

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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BookmarksViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: VaultDatabase
    private lateinit var repository: NotificationRepository
    private lateinit var viewModel: BookmarksViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = RuntimeEnvironment.getApplication()
        database = VaultDatabase.createInMemoryDatabase(context)
        repository = NotificationRepositoryImpl(database.notificationDao(), testDispatcher)
        viewModel = BookmarksViewModel(repository)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_whenNoBookmarks_emitsEmpty() = runTest(testDispatcher) {
        val nonBookmarked = ArchivedNotification(
            id = "notif_1",
            packageName = "com.test.app",
            title = "Unbookmarked Title",
            body = "Body",
            isBookmarked = false,
            eventTimeMs = 1000L,
            capturedAtMs = 1000L
        )
        repository.saveNotification(nonBookmarked)

        val state = viewModel.uiState.first { it !is BookmarksUiState.Loading }
        assertTrue("Expected BookmarksUiState.Empty when no bookmarked items exist", state is BookmarksUiState.Empty)
    }

    @Test
    fun uiState_whenBookmarksExist_emitsSuccessWithOnlyBookmarkedItems() = runTest(testDispatcher) {
        val bookmarked = ArchivedNotification(
            id = "bookmarked_1",
            packageName = "com.test.app",
            title = "Bookmarked",
            body = "Body",
            isBookmarked = true,
            eventTimeMs = 2000L,
            capturedAtMs = 2000L
        )
        val notBookmarked = ArchivedNotification(
            id = "regular_1",
            packageName = "com.test.app",
            title = "Regular",
            body = "Body",
            isBookmarked = false,
            eventTimeMs = 1000L,
            capturedAtMs = 1000L
        )

        repository.saveNotification(bookmarked)
        repository.saveNotification(notBookmarked)

        val state = viewModel.uiState.first { it !is BookmarksUiState.Loading }
        assertTrue("State should be BookmarksUiState.Success", state is BookmarksUiState.Success)
        val successState = state as BookmarksUiState.Success
        assertEquals(1, successState.notifications.size)
        assertEquals("bookmarked_1", successState.notifications.first().id)
        assertTrue(successState.notifications.first().isBookmarked)
    }

    @Test
    fun toggleBookmark_whenUnbookmarked_immediatelyRemovesFromStream() = runTest(testDispatcher) {
        val bookmarked = ArchivedNotification(
            id = "item_1",
            packageName = "com.test.app",
            title = "To Unstar",
            body = "Body",
            isBookmarked = true,
            eventTimeMs = 1000L,
            capturedAtMs = 1000L
        )
        repository.saveNotification(bookmarked)

        val stateBefore = viewModel.uiState.first { it is BookmarksUiState.Success }
        assertEquals(1, (stateBefore as BookmarksUiState.Success).notifications.size)

        // Toggle bookmark off
        val job = viewModel.toggleBookmark("item_1", currentBookmark = true)
        job.join()
        advanceUntilIdle()

        val stateAfter = viewModel.uiState.first { it is BookmarksUiState.Empty }
        assertTrue("State after removing sole bookmark should be Empty", stateAfter is BookmarksUiState.Empty)
    }

    @Test
    fun requestDelete_setsDeleteCandidate_andDismissDeleteClearsCandidate() = runTest(testDispatcher) {
        val notification = ArchivedNotification(
            id = "item_to_delete",
            packageName = "com.test.app",
            title = "Delete Me",
            body = "Body",
            isBookmarked = true,
            eventTimeMs = 1000L,
            capturedAtMs = 1000L
        )

        assertNull(viewModel.deleteCandidate.value)

        viewModel.requestDelete(notification)
        assertEquals(notification, viewModel.deleteCandidate.value)

        viewModel.dismissDelete()
        assertNull(viewModel.deleteCandidate.value)
    }

    @Test
    fun confirmDelete_deletesFromRepository_andClearsCandidate() = runTest(testDispatcher) {
        val notification = ArchivedNotification(
            id = "item_to_delete_2",
            packageName = "com.test.app",
            title = "Delete Permanently",
            body = "Body",
            isBookmarked = true,
            eventTimeMs = 1000L,
            capturedAtMs = 1000L
        )
        repository.saveNotification(notification)

        viewModel.requestDelete(notification)
        assertNotNull(viewModel.deleteCandidate.value)

        val job = viewModel.confirmDelete()
        job.join()
        advanceUntilIdle()

        assertNull(viewModel.deleteCandidate.value)
        val stateAfter = viewModel.uiState.first { it is BookmarksUiState.Empty }
        assertTrue("Should be empty after deleting the only record", stateAfter is BookmarksUiState.Empty)
        assertNull(repository.getNotificationByIdOnce("item_to_delete_2"))
    }
}
