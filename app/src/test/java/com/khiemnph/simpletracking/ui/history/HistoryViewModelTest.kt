package com.khiemnph.simpletracking.ui.history

import app.cash.turbine.test
import com.khiemnph.domain.fake.MockedSessionRepository
import com.khiemnph.domain.interactor.ObserveSessionHistoryUseCase
import com.khiemnph.domain.model.SessionSummary
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val repository = MockedSessionRepository()
    private val viewModel by lazy { HistoryViewModel(ObserveSessionHistoryUseCase(repository)) }

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
        thumbnailPath: String? = null,
        recordedAt: Long = 0L,
    ) = SessionSummary(
        id = id,
        distanceMeters = distanceMeters,
        durationMillis = durationMillis,
        averageSpeedMps = averageSpeedMps,
        thumbnailPath = thumbnailPath,
        recordedAt = recordedAt,
    )

    private suspend fun stopASession(): String {
        val sessionId = repository.startSession()
        repository.stopSession(
            sessionId = sessionId,
            thumbnailPath = null,
            finalDistanceMeters = 1_000.0,
        )
        return sessionId
    }

    @Test
    fun givenNoSessionSummaries_whenUiStateCollected_thenListIsEmpty() = runTest {
        viewModel.uiState.test {
            assertEquals(emptyList<HistorySummaryUiModel>(), awaitItem())
        }
    }

    @Test
    fun givenRepositoryEmitsThreeSummaries_whenUiStateCollected_thenListOrderMatchesRepositoryOrder() = runTest {
        val firstId = stopASession()
        val secondId = stopASession()
        val thirdId = stopASession()

        viewModel.uiState.test {
            val summaries = awaitItem()
            assertEquals(listOf(firstId, secondId, thirdId), summaries.map { it.id })
        }
    }

    @Test
    fun givenDistanceOf5420Meters_whenMappedToUiModel_thenDistanceLabelIsFormattedInKilometersWithTwoDecimals() {
        val uiModel = sessionSummary(distanceMeters = 5420.0).toHistorySummaryUiModel()

        assertEquals("5.42 km", uiModel.distanceLabel)
    }

    @Test
    fun givenDurationUnder1Hour_whenMappedToUiModel_thenDurationLabelIsFormattedAsMinutesColonSeconds() {
        val uiModel = sessionSummary(durationMillis = 1_938_000L).toHistorySummaryUiModel()

        assertEquals("32:18", uiModel.durationLabel)
    }

    @Test
    fun givenDurationOver1Hour_whenMappedToUiModel_thenDurationLabelIncludesHours() {
        val uiModel = sessionSummary(durationMillis = 3_725_000L).toHistorySummaryUiModel()

        assertEquals("1:02:05", uiModel.durationLabel)
    }

    @Test
    fun givenAverageSpeedOf2Point5Mps_whenMappedToUiModel_thenAverageSpeedLabelIsConvertedToKmPerHour() {
        val uiModel = sessionSummary(averageSpeedMps = 2.5f).toHistorySummaryUiModel()

        assertEquals("9.0 km/h avg", uiModel.averageSpeedLabel)
    }

    @Test
    fun givenSummaryWithKnownRecordedAt_whenMappedToUiModel_thenRecordedAtLabelIsFormattedAsDayAndTime() {
        // 2024-01-02T07:12:00Z is a Tuesday; a fixed UTC zone keeps this test timezone-independent.
        val recordedAtMillis = 1_704_179_520_000L
        val uiModel = sessionSummary(recordedAt = recordedAtMillis)
            .toHistorySummaryUiModel(zoneId = ZoneId.of("UTC"))

        assertEquals("Tue, 7:12 AM", uiModel.recordedAtLabel)
    }

    @Test
    fun givenNullThumbnailPath_whenMappedToUiModel_thenThumbnailPathRemainsNull() {
        val uiModel = sessionSummary(thumbnailPath = null).toHistorySummaryUiModel()

        assertNull(uiModel.thumbnailPath)
    }

    @Test
    fun givenNonNullThumbnailPath_whenMappedToUiModel_thenThumbnailPathIsPassedThroughUnchanged() {
        val uiModel = sessionSummary(thumbnailPath = "/data/thumbnails/session-1.png").toHistorySummaryUiModel()

        assertEquals("/data/thumbnails/session-1.png", uiModel.thumbnailPath)
    }
}
