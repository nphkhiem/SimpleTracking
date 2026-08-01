package com.khiemnph.simpletracking.ui.summary

import androidx.lifecycle.SavedStateHandle
import com.khiemnph.domain.fake.MockedSessionRepository
import com.khiemnph.domain.interactor.DeleteSessionUseCase
import com.khiemnph.domain.interactor.ObserveSessionSummaryUseCase
import com.khiemnph.domain.model.SessionSummary
import com.khiemnph.simpletracking.testing.DefaultLocaleRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SummaryViewModelTest {

    @get:Rule
    val localeRule = DefaultLocaleRule()

    private val dispatcher = StandardTestDispatcher()
    private val repository = MockedSessionRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun seed(
        id: String = "session-1",
        distanceMeters: Double = 5_420.0,
        durationMillis: Long = 1_694_000L,
        routePolyline: String? = null,
    ) {
        repository.seedSummaries(
            listOf(
                SessionSummary(
                    id = id,
                    distanceMeters = distanceMeters,
                    durationMillis = durationMillis,
                    averageSpeedMps = 3.2f,
                    routePolyline = routePolyline,
                    recordedAt = 1_700_000_000_000L,
                ),
            ),
        )
    }

    private fun viewModel(sessionId: String = "session-1") = SummaryViewModel(
        observeSessionSummaryUseCase = ObserveSessionSummaryUseCase(repository),
        deleteSessionUseCase = DeleteSessionUseCase(repository),
        savedStateHandle = SavedStateHandle(mapOf("sessionId" to sessionId)),
    )

    @Test
    fun `formats the finished run's numbers`() = runTest {
        seed()

        val state = viewModel().also { advanceUntilIdle() }.uiState.value

        val ready = state as SummaryUiState.Ready
        assertEquals("5.42", ready.distanceKm)
        assertEquals("28:14", ready.durationLabel)
    }

    @Test
    fun `pace comes from distance over time so it agrees with the other two numbers`() = runTest {
        // 5420 m in 1694 s is 3.1995 m/s, so 312.6 s/km, which rounds to 5:13.
        seed()

        val ready = viewModel().also { advanceUntilIdle() }.uiState.value as SummaryUiState.Ready

        assertEquals("5:13", ready.paceLabel)
    }



    @Test
    fun `an unknown session reports not found rather than crashing`() = runTest {
        val state = viewModel(sessionId = "no-such-session").also { advanceUntilIdle() }.uiState.value

        assertEquals(SummaryUiState.NotFound, state)
    }

    @Test
    fun `discarding removes the run and then reports back`() = runTest {
        seed()
        val vm = viewModel().also { advanceUntilIdle() }
        var reported = false

        vm.onDiscard { reported = true }
        advanceUntilIdle()

        assertTrue("the caller must be told only after the delete has run", reported)
        assertEquals(null, repository.observeSessionSummary("session-1").first())
    }

    /**
     * The bug this screen shipped with on first attempt, found on device rather than in tests.
     *
     * Stop dispatches an intent and navigates immediately, so this screen opens while the Service
     * is still writing the final stats. Reading once reported a run that did exist as missing.
     */
    @Test
    fun `waits for the stop to land rather than reporting a run that is still being written`() = runTest {
        // runCurrent rather than advanceUntilIdle: the latter would fast-forward virtual time past
        // the wait window and prove only that the timeout works, which the unknown-session test
        // above already covers. This models the write landing promptly, as it does in practice.
        val vm = viewModel()
        runCurrent()
        assertEquals("still writing, so not resolved yet", SummaryUiState.Loading, vm.uiState.value)

        seed()
        runCurrent()

        val ready = vm.uiState.value as SummaryUiState.Ready
        assertEquals("5.42", ready.distanceKm)
    }
}
