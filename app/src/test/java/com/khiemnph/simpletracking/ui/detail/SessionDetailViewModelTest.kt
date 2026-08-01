package com.khiemnph.simpletracking.ui.detail

import androidx.lifecycle.SavedStateHandle
import com.khiemnph.domain.fake.MockedSessionRepository
import com.khiemnph.domain.interactor.DeleteSessionUseCase
import com.khiemnph.domain.interactor.ExportSessionGpxUseCase
import com.khiemnph.domain.interactor.GetSessionSplitsUseCase
import com.khiemnph.domain.interactor.ObserveSessionSummaryUseCase
import com.khiemnph.domain.interactor.RenameSessionUseCase
import com.khiemnph.domain.model.LocationPoint
import com.khiemnph.domain.model.SessionSummary
import com.khiemnph.simpletracking.testing.DefaultLocaleRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Must match [DistanceCalculator]'s own earth model, or a fixture that says "2000 m" measures
 * something else and split boundaries land in the wrong place. Derived rather than written out, so
 * the two cannot drift apart.
 */
private val METERS_PER_DEGREE_LATITUDE = 6_371_000.0 * Math.PI / 180.0

@OptIn(ExperimentalCoroutinesApi::class)
class SessionDetailViewModelTest {

    @get:Rule
    val localeRule = DefaultLocaleRule()

    private val dispatcher = StandardTestDispatcher()
    private val repository = MockedSessionRepository()
    private val sessionId = "session-1"

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun point(northMeters: Double, atMillis: Long) = LocationPoint(
        sessionId = sessionId,
        latitude = northMeters / METERS_PER_DEGREE_LATITUDE,
        longitude = 0.0,
        timestamp = atMillis,
        horizontalAccuracyMeters = 5f,
        speedMetersPerSec = 3f,
    )

    /** A run of [totalMeters] at [metersPerSecond], sampled every second. */
    private fun seedRun(totalMeters: Double, metersPerSecond: Double) {
        val seconds = (totalMeters / metersPerSecond).toInt()
        repository.seedPoints(sessionId, (0..seconds).map { s -> point(s * metersPerSecond, s * 1_000L) })
        repository.seedSummaries(
            listOf(
                SessionSummary(
                    id = sessionId,
                    distanceMeters = totalMeters,
                    durationMillis = seconds * 1_000L,
                    averageSpeedMps = metersPerSecond.toFloat(),
                    routePolyline = null,
                    recordedAt = 1_700_000_000_000L,
                ),
            ),
        )
    }

    private fun viewModel(id: String = sessionId) = SessionDetailViewModel(
        observeSessionSummaryUseCase = ObserveSessionSummaryUseCase(repository),
        getSessionSplitsUseCase = GetSessionSplitsUseCase(repository),
        exportSessionGpxUseCase = ExportSessionGpxUseCase(repository),
        renameSessionUseCase = RenameSessionUseCase(repository),
        deleteSessionUseCase = DeleteSessionUseCase(repository),
        savedStateHandle = SavedStateHandle(mapOf("sessionId" to id)),
    )

    @Test
    fun `a two kilometre run reports two splits`() = runTest {
        seedRun(2_000.0, 4.0)

        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value as SessionDetailUiState.Ready
        assertEquals(2, state.splits.size)
        assertEquals(listOf("1", "2"), state.splits.map { it.label })
    }

    @Test
    fun `a partial closing split is labelled by its distance, not a kilometre number`() = runTest {
        seedRun(2_500.0, 4.0)

        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value as SessionDetailUiState.Ready
        assertEquals(3, state.splits.size)
        assertEquals("0.50", state.splits.last().label)
    }

    @Test
    fun `the fastest complete split is marked, and only one of them`() = runTest {
        repository.seedPoints(
            sessionId,
            (0..250).map { s -> point(s * 4.0, s * 1_000L) } +
                (1..125).map { s -> point(1_000.0 + s * 8.0, 250_000L + s * 1_000L) },
        )
        repository.seedSummaries(
            listOf(
                SessionSummary(sessionId, 2_000.0, 375_000L, 5.3f, null, 1_700_000_000_000L),
            ),
        )

        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value as SessionDetailUiState.Ready
        assertEquals(1, state.splits.count { it.isFastest })
        assertTrue("the second kilometre was faster", state.splits[1].isFastest)
    }

    @Test
    fun `best pace ignores a partial split, which would otherwise invent a pace never run`() = runTest {
        // A 20 m closing split covered in 2 s extrapolates to a 1:40 km. Letting that win would be a lie.
        repository.seedPoints(
            sessionId,
            (0..250).map { s -> point(s * 4.0, s * 1_000L) } +
                listOf(point(1_020.0, 252_000L)),
        )
        repository.seedSummaries(
            listOf(SessionSummary(sessionId, 1_020.0, 252_000L, 4.0f, null, 1_700_000_000_000L)),
        )

        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value as SessionDetailUiState.Ready
        assertEquals("the only complete kilometre was run at 4 m/s", "4:10", state.bestPaceLabel)
    }

    @Test
    fun `slower splits get longer bars`() = runTest {
        repository.seedPoints(
            sessionId,
            (0..250).map { s -> point(s * 4.0, s * 1_000L) } +
                (1..125).map { s -> point(1_000.0 + s * 8.0, 250_000L + s * 1_000L) },
        )
        repository.seedSummaries(
            listOf(SessionSummary(sessionId, 2_000.0, 375_000L, 5.3f, null, 1_700_000_000_000L)),
        )

        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value as SessionDetailUiState.Ready
        assertTrue("the slower kilometre is the longer bar", state.splits[0].barFraction > state.splits[1].barFraction)
        assertEquals("the slowest split fills the row", 1f, state.splits[0].barFraction, 0.001f)
    }

    @Test
    fun `an unknown session reports not found`() = runTest {
        val vm = viewModel(id = "no-such-session")
        advanceUntilIdle()

        assertEquals(SessionDetailUiState.NotFound, vm.uiState.value)
    }

    @Test
    fun `a session deleted while open collapses to not found rather than showing stale stats`() = runTest {
        seedRun(2_000.0, 4.0)
        val vm = viewModel()
        advanceUntilIdle()
        assertTrue(vm.uiState.value is SessionDetailUiState.Ready)

        repository.deleteSession(sessionId)
        advanceUntilIdle()

        assertEquals(SessionDetailUiState.NotFound, vm.uiState.value)
    }

    @Test
    fun `deleting removes the run and then reports back`() = runTest {
        seedRun(2_000.0, 4.0)
        val vm = viewModel()
        advanceUntilIdle()
        var reported = false

        vm.onDelete { reported = true }
        advanceUntilIdle()

        assertTrue(reported)
    }

    @Test
    fun `an unnamed run shows its date as the title`() = runTest {
        seedRun(2_000.0, 4.0)

        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value as SessionDetailUiState.Ready
        assertEquals(false, state.hasCustomTitle)
        assertTrue("expected a date, got ${state.titleLabel}", state.titleLabel.contains(","))
    }

    @Test
    fun `renaming replaces the date with the chosen name`() = runTest {
        seedRun(2_000.0, 4.0)
        val vm = viewModel()
        advanceUntilIdle()

        vm.onRename("Morning loop")
        advanceUntilIdle()

        val state = vm.uiState.value as SessionDetailUiState.Ready
        assertEquals("Morning loop", state.titleLabel)
        assertEquals(true, state.hasCustomTitle)
    }
}
