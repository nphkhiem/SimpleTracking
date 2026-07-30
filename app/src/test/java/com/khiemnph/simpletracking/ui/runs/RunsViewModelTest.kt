package com.khiemnph.simpletracking.ui.runs

import app.cash.turbine.test
import com.khiemnph.domain.fake.MockedSessionRepository
import com.khiemnph.domain.interactor.DeleteSessionUseCase
import com.khiemnph.domain.interactor.ObserveSessionHistoryUseCase
import com.khiemnph.domain.model.SessionSummary
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Before
import com.khiemnph.simpletracking.testing.DefaultLocaleRule
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RunsViewModelTest {

    @get:Rule
    val localeRule = DefaultLocaleRule()

    private val repository = MockedSessionRepository()
    private val viewModel by lazy {
        RunsViewModel(ObserveSessionHistoryUseCase(repository), DeleteSessionUseCase(repository))
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sessionSummary(
        id: String = "session-1",
        distanceMeters: Double = 1_000.0,
        durationMillis: Long = 60_000L,
        averageSpeedMps: Float = 1f,
        routePolyline: String? = null,
        recordedAt: Long = 0L,
    ) = SessionSummary(
        id = id,
        distanceMeters = distanceMeters,
        durationMillis = durationMillis,
        averageSpeedMps = averageSpeedMps,
        routePolyline = routePolyline,
        recordedAt = recordedAt,
    )

    private suspend fun stopASession(): String {
        val sessionId = repository.startSession()
        repository.stopSession(
            sessionId = sessionId,
            routePolyline = null,
            finalDistanceMeters = 1_000.0,
        )
        return sessionId
    }

    @Test
    fun givenASwipedSession_whenTheUndoWindowIsStillOpen_thenItIsHiddenButNotYetDeleted() = runTest {
        val sessionId = stopASession()
        viewModel.uiState.test { awaitItem() }

        viewModel.onSessionSwipedAway(sessionId)

        assertEquals(RunsUiState.Empty, viewModel.uiState.value)
        // Still on disk: the row is hidden optimistically so Undo has something to bring back.
        assertEquals(1, repository.observeSessionSummaries().first().size)
    }

    @Test
    fun givenASwipedSession_whenTheUndoWindowElapses_thenItIsActuallyDeleted() = runTest {
        val sessionId = stopASession()
        viewModel.uiState.test { awaitItem() }

        viewModel.onSessionSwipedAway(sessionId)
        advanceTimeBy(6_000L)
        runCurrent()

        assertTrue(repository.observeSessionSummaries().first().isEmpty())
    }

    /** The whole point of Undo: nothing recorded may be lost by a swipe the user takes back. */
    @Test
    fun givenASwipedSession_whenUndoneBeforeTheWindowElapses_thenItComesBackAndSurvives() = runTest {
        val sessionId = stopASession()
        viewModel.uiState.test { awaitItem() }

        viewModel.onSessionSwipedAway(sessionId)
        viewModel.onUndoDelete(sessionId)
        advanceTimeBy(6_000L)
        runCurrent()

        assertEquals(listOf(sessionId), viewModel.uiState.value.sessionsOrEmpty().map { it.id })
        assertEquals(1, repository.observeSessionSummaries().first().size)
    }

    @Test
    fun givenTwoSessions_whenOneIsSwipedAway_thenTheOtherStaysVisible() = runTest {
        val kept = stopASession()
        val swiped = stopASession()
        viewModel.uiState.test { awaitItem() }

        viewModel.onSessionSwipedAway(swiped)

        assertEquals(listOf(kept), viewModel.uiState.value.sessionsOrEmpty().map { it.id })
    }

    @Test
    fun givenNoSessionSummaries_whenUiStateCollected_thenTheStateIsEmptyRatherThanAnEmptyList() = runTest {
        viewModel.uiState.test {
            assertEquals(RunsUiState.Empty, awaitItem())
        }
    }

    @Test
    fun givenRepositoryEmitsThreeSummaries_whenUiStateCollected_thenListOrderMatchesRepositoryOrder() = runTest {
        val firstId = stopASession()
        val secondId = stopASession()
        val thirdId = stopASession()

        viewModel.uiState.test {
            val sessions = awaitItem().sessionsOrEmpty()
            assertEquals(listOf(firstId, secondId, thirdId), sessions.map { it.id })
        }
    }

    @Test
    fun givenDistanceOf5420Meters_whenMappedToUiModel_thenDistanceLabelIsFormattedInKilometersWithTwoDecimals() {
        val uiModel = sessionSummary(distanceMeters = 5420.0).toRunSummaryUiModel()

        assertEquals("5.42", uiModel.distanceKm)
    }

    @Test
    fun givenDurationUnder1Hour_whenMappedToUiModel_thenDurationLabelIsFormattedAsMinutesColonSeconds() {
        val uiModel = sessionSummary(durationMillis = 1_938_000L).toRunSummaryUiModel()

        assertEquals("32:18", uiModel.durationLabel)
    }

    @Test
    fun givenDurationOver1Hour_whenMappedToUiModel_thenDurationLabelIncludesHours() {
        val uiModel = sessionSummary(durationMillis = 3_725_000L).toRunSummaryUiModel()

        assertEquals("1:02:05", uiModel.durationLabel)
    }

    @Test
    fun givenAverageSpeedOf2Point5Mps_whenMappedToUiModel_thenAverageSpeedLabelIsConvertedToKmPerHour() {
        val uiModel = sessionSummary(averageSpeedMps = 2.5f).toRunSummaryUiModel()

        assertEquals("9.0", uiModel.averageSpeedKmh)
    }

    @Test
    fun givenSummaryWithKnownRecordedAt_whenMappedToUiModel_thenRecordedAtLabelIsFormattedAsDayAndTime() {
        // 2024-01-02T07:12:00Z is a Tuesday; a fixed UTC zone keeps this test timezone-independent.
        val recordedAtMillis = 1_704_179_520_000L
        val uiModel = sessionSummary(recordedAt = recordedAtMillis)
            .toRunSummaryUiModel(zoneId = ZoneId.of("UTC"))

        assertEquals("Tue, 7:12 AM", uiModel.recordedAtLabel)
    }

    @Test
    fun givenNoRecordedRoute_whenMappedToUiModel_thenThereAreNoPointsToDraw() {
        val uiModel = sessionSummary(routePolyline = null).toRunSummaryUiModel()

        assertTrue(uiModel.routePoints.isEmpty())
    }

    @Test
    fun givenARecordedRoute_whenMappedToUiModel_thenItIsDecodedReadyForDrawing() {
        // Decoding here rather than in the composable keeps the screen dumb: it draws points, it
        // does not parse storage formats.
        val uiModel = sessionSummary(routePolyline = "2103850,10585420;2103900,10585470")
            .toRunSummaryUiModel()

        assertEquals(2, uiModel.routePoints.size)
        assertEquals(21.0385, uiModel.routePoints.first().latitude, 0.0001)
    }

    @Test
    fun givenAnUnparseableRoute_whenMappedToUiModel_thenTheRowStillRendersWithNoRoute() {
        val uiModel = sessionSummary(routePolyline = "corrupt").toRunSummaryUiModel()

        assertTrue(uiModel.routePoints.isEmpty())
    }

    /** Flattens the day groups, so assertions about which runs are visible stay readable. */
    private fun RunsUiState.sessionsOrEmpty(): List<RunSummaryUiModel> =
        (this as? RunsUiState.Sessions)?.groups?.flatMap { it.sessions }.orEmpty()
}
