package com.example.notificationvault.ui.inbox

import com.example.notificationvault.data.local.VaultDatabase
import com.example.notificationvault.data.repository.NotificationRepositoryImpl
import com.example.notificationvault.domain.model.ArchivedNotification
import com.example.notificationvault.domain.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class InboxViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: VaultDatabase
    private lateinit var repository: NotificationRepository
    private lateinit var viewModel: InboxViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = RuntimeEnvironment.getApplication()
        database = VaultDatabase.createInMemoryDatabase(context)
        repository = NotificationRepositoryImpl(database.notificationDao(), testDispatcher)
        viewModel = InboxViewModel(repository)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_whenNoNotifications_emitsEmpty() = runTest(testDispatcher) {
        val state = viewModel.uiState.first { it !is InboxUiState.Loading }
        assertTrue(state is InboxUiState.Empty)
    }

    @Test
    fun uiState_whenNotificationsExist_emitsSuccessWithChronologicalOrder() = runTest(testDispatcher) {
        val item1 = ArchivedNotification(
            id = "id_1",
            packageName = "com.test.one",
            title = "First",
            body = "Body 1",
            eventTimeMs = 1000L,
            capturedAtMs = 1000L
        )
        val item2 = ArchivedNotification(
            id = "id_2",
            packageName = "com.test.two",
            title = "Second",
            body = "Body 2",
            eventTimeMs = 2000L,
            capturedAtMs = 2000L
        )

        repository.saveNotification(item1)
        repository.saveNotification(item2)

        val state = viewModel.uiState.first { it !is InboxUiState.Loading }
        assertTrue(state is InboxUiState.Success)
        val list = (state as InboxUiState.Success).notifications
        assertEquals(2, list.size)
        // Ordered newest-first by capturedAtMs
        assertEquals("id_2", list[0].id)
        assertEquals("id_1", list[1].id)
    }

    @Test
    fun toggleBookmark_togglesBookmarkInRepository() = runTest(testDispatcher) {
        val item = ArchivedNotification(
            id = "id_bookmark",
            packageName = "com.test.app",
            title = "Notice",
            body = "Check this",
            eventTimeMs = 1000L,
            capturedAtMs = 1000L,
            isBookmarked = false
        )
        repository.saveNotification(item)

        val job1 = viewModel.toggleBookmark("id_bookmark", currentBookmark = false)
        job1.join()

        val updated = repository.getNotificationByIdOnce("id_bookmark")
        assertTrue(updated?.isBookmarked == true)

        val job2 = viewModel.toggleBookmark("id_bookmark", currentBookmark = true)
        job2.join()

        val toggledBack = repository.getNotificationByIdOnce("id_bookmark")
        assertTrue(toggledBack?.isBookmarked == false)
    }
}
