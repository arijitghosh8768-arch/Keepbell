package com.example.notificationvault.ui.inbox

import com.example.notificationvault.data.local.VaultDatabase
import com.example.notificationvault.data.repository.NotificationRepositoryImpl
import com.example.notificationvault.domain.model.ArchivedNotification
import com.example.notificationvault.domain.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
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
import java.time.Instant
import java.time.ZoneId

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
    fun searchFiltering_matchesTitleBodyOrAppLabel_withDebounce() = runTest(testDispatcher) {
        val item1 = ArchivedNotification(
            id = "id_1",
            packageName = "com.bank.app",
            appLabel = "Bank App",
            title = "Account Statement",
            body = "Here is your statement",
            eventTimeMs = 1000L,
            capturedAtMs = 1000L
        )
        val item2 = ArchivedNotification(
            id = "id_2",
            packageName = "com.chat.app",
            appLabel = "Messenger",
            title = "Hi",
            body = "See you later",
            eventTimeMs = 2000L,
            capturedAtMs = 2000L
        )
        repository.saveNotification(item1)
        repository.saveNotification(item2)

        // Type search query
        viewModel.onSearchQueryChange("Statement")
        advanceTimeBy(350)
        advanceUntilIdle()

        val state = viewModel.uiState.first { it is InboxUiState.Success } as InboxUiState.Success
        assertEquals(1, state.notifications.size)
        assertEquals("id_1", state.notifications[0].id)
    }

    @Test
    fun packageFiltering_isolatesTargetApp() = runTest(testDispatcher) {
        val item1 = ArchivedNotification(
            id = "id_1",
            packageName = "com.app.target",
            title = "Target 1",
            eventTimeMs = 1000L,
            capturedAtMs = 1000L
        )
        val item2 = ArchivedNotification(
            id = "id_2",
            packageName = "com.app.other",
            title = "Other 1",
            eventTimeMs = 2000L,
            capturedAtMs = 2000L
        )
        repository.saveNotification(item1)
        repository.saveNotification(item2)

        viewModel.onPackageFilterSelect("com.app.target")
        advanceUntilIdle()

        val state = viewModel.uiState.first { it is InboxUiState.Success } as InboxUiState.Success
        assertEquals(1, state.notifications.size)
        assertEquals("id_1", state.notifications[0].id)
    }

    @Test
    fun datePresetFiltering_todayOnlyCapturesTodayRecords() = runTest(testDispatcher) {
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val (startOfToday, _) = DateFilterPreset.TODAY.calculateBoundaries(now, zone)

        val oldItem = ArchivedNotification(
            id = "old_item",
            packageName = "com.app",
            title = "Old",
            eventTimeMs = (startOfToday ?: 0L) - 100_000L,
            capturedAtMs = (startOfToday ?: 0L) - 100_000L
        )
        val todayItem = ArchivedNotification(
            id = "today_item",
            packageName = "com.app",
            title = "Today",
            eventTimeMs = (startOfToday ?: 0L) + 5_000L,
            capturedAtMs = (startOfToday ?: 0L) + 5_000L
        )
        repository.saveNotification(oldItem)
        repository.saveNotification(todayItem)

        viewModel.onDatePresetSelect(DateFilterPreset.TODAY)
        advanceUntilIdle()

        val state = viewModel.uiState.first { it is InboxUiState.Success } as InboxUiState.Success
        assertEquals(1, state.notifications.size)
        assertEquals("today_item", state.notifications[0].id)
    }

    @Test
    fun combinedFilters_intersectCorrectly() = runTest(testDispatcher) {
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val (startOfToday, _) = DateFilterPreset.TODAY.calculateBoundaries(now, zone)

        val matchBoth = ArchivedNotification(
            id = "match_both",
            packageName = "com.work.chat",
            title = "Daily Standup Meeting",
            body = "Join call",
            eventTimeMs = (startOfToday ?: 0L) + 1_000L,
            capturedAtMs = (startOfToday ?: 0L) + 1_000L
        )
        val matchAppOnly = ArchivedNotification(
            id = "match_app_only",
            packageName = "com.work.chat",
            title = "Lunch",
            body = "Join for lunch",
            eventTimeMs = (startOfToday ?: 0L) + 2_000L,
            capturedAtMs = (startOfToday ?: 0L) + 2_000L
        )
        val matchQueryOnly = ArchivedNotification(
            id = "match_query_only",
            packageName = "com.personal.chat",
            title = "Meeting reminder",
            body = "Coffee meeting",
            eventTimeMs = (startOfToday ?: 0L) + 3_000L,
            capturedAtMs = (startOfToday ?: 0L) + 3_000L
        )

        repository.saveNotification(matchBoth)
        repository.saveNotification(matchAppOnly)
        repository.saveNotification(matchQueryOnly)

        viewModel.onPackageFilterSelect("com.work.chat")
        viewModel.onDatePresetSelect(DateFilterPreset.TODAY)
        viewModel.onSearchQueryChange("Standup")
        advanceTimeBy(350)
        advanceUntilIdle()

        val state = viewModel.uiState.first { it is InboxUiState.Success } as InboxUiState.Success
        assertEquals(1, state.notifications.size)
        assertEquals("match_both", state.notifications[0].id)
    }

    @Test
    fun noResultsState_distinguishedFromEmptyArchive() = runTest(testDispatcher) {
        val item = ArchivedNotification(
            id = "id_exist",
            packageName = "com.example.app",
            title = "Existing Alert",
            eventTimeMs = 1000L,
            capturedAtMs = 1000L
        )
        repository.saveNotification(item)

        viewModel.onSearchQueryChange("NonExistentTerm")
        advanceTimeBy(350)
        advanceUntilIdle()

        val state = viewModel.uiState.first { it !is InboxUiState.Loading }
        assertTrue("Expected NoResults state when archive is non-empty", state is InboxUiState.NoResults)
        assertEquals("NonExistentTerm", (state as InboxUiState.NoResults).query)
    }

    @Test
    fun clearFilters_restoresFullArchive() = runTest(testDispatcher) {
        val item1 = ArchivedNotification(id = "1", packageName = "com.a", title = "Alert 1", eventTimeMs = 1L, capturedAtMs = 1L)
        val item2 = ArchivedNotification(id = "2", packageName = "com.b", title = "Alert 2", eventTimeMs = 2L, capturedAtMs = 2L)
        repository.saveNotification(item1)
        repository.saveNotification(item2)

        viewModel.onSearchQueryChange("Alert 1")
        advanceTimeBy(350)
        advanceUntilIdle()

        val filteredState = viewModel.uiState.first { it is InboxUiState.Success } as InboxUiState.Success
        assertEquals(1, filteredState.notifications.size)

        // Clear filters
        viewModel.clearFilters()
        advanceTimeBy(350)
        advanceUntilIdle()

        val restoredState = viewModel.uiState.first { it is InboxUiState.Success && it.notifications.size == 2 } as InboxUiState.Success
        assertEquals(2, restoredState.notifications.size)
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

    @Test
    fun uiState_whenClearAllInvoked_reactivelyTransitionsFromSuccessToEmpty() = runTest(testDispatcher) {
        val item1 = ArchivedNotification(
            id = "id_1",
            packageName = "com.test.app",
            title = "Notice 1",
            body = "Body 1",
            eventTimeMs = 1000L,
            capturedAtMs = 1000L
        )
        val item2 = ArchivedNotification(
            id = "id_2",
            packageName = "com.test.app",
            title = "Notice 2",
            body = "Body 2",
            eventTimeMs = 2000L,
            capturedAtMs = 2000L,
            isBookmarked = true
        )
        repository.saveNotification(item1)
        repository.saveNotification(item2)

        val initialState = viewModel.uiState.first { it is InboxUiState.Success }
        assertTrue("Inbox must initially be in Success state", initialState is InboxUiState.Success)
        assertEquals(2, (initialState as InboxUiState.Success).notifications.size)

        // Simulate Clear All invoked from Settings/Repository
        repository.deleteAllNotifications()

        val clearedState = viewModel.uiState.first { it is InboxUiState.Empty }
        assertTrue("Inbox must reactively transition to Empty state upon Clear All", clearedState is InboxUiState.Empty)
    }
}
