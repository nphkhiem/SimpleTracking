package com.khiemnph.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.khiemnph.data.local.db.AppDatabase
import com.khiemnph.data.local.db.LocationPointDao
import com.khiemnph.data.local.db.SessionDao
import com.khiemnph.data.util.Clock
import com.khiemnph.domain.model.LocationPoint
import com.khiemnph.domain.model.SessionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Runs Room's query/transaction dispatch synchronously so Flow emissions aren't racing real
 * background threads against `runTest`'s virtual clock. */
private val immediateExecutor = Executor { it.run() }

private class FakeClock(var currentMillis: Long = 0L) : Clock {
    override fun nowMillis(): Long = currentMillis
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SessionRepositoryImplTest {

    private lateinit var database: AppDatabase
    private lateinit var sessionDao: SessionDao
    private lateinit var locationPointDao: LocationPointDao
    private val clock = FakeClock()
    private lateinit var repository: SessionRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(immediateExecutor)
            .setTransactionExecutor(immediateExecutor)
            .build()
        sessionDao = database.sessionDao()
        locationPointDao = database.locationPointDao()
        repository = SessionRepositoryImpl(sessionDao, locationPointDao, clock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun givenNoActiveSession_whenStartSession_thenCreatesRunningSessionAndReturnsItsId() = runTest {
        clock.currentMillis = 1_000L

        val sessionId = repository.startSession()

        assertEquals(sessionId, repository.getActiveSessionId())
        val state = repository.observeActiveSession().first()
        assertEquals(sessionId, state?.session?.id)
        assertEquals(SessionStatus.RUNNING, state?.session?.status)
    }

    @Test
    fun givenNoActiveSession_whenObserveActiveSession_thenEmitsNull() = runTest {
        assertNull(repository.observeActiveSession().first())
    }

    @Test
    fun givenSessionPausedThenResumed_whenDurationComputed_thenPausedIntervalExcludedFromElapsedTime() = runTest {
        clock.currentMillis = 0L
        val sessionId = repository.startSession()

        clock.currentMillis = 5_000L
        repository.pauseSession(sessionId)

        clock.currentMillis = 8_000L
        repository.resumeSession(sessionId)

        clock.currentMillis = 10_000L
        val state = repository.observeActiveSession().first()

        // elapsed = now(10000) - start(0) - pausedDuration(8000-5000=3000) = 7000
        assertEquals(7_000L, state?.elapsedDurationMillis)
    }

    @Test
    fun givenSessionPausedTwice_whenDurationComputed_thenBothPausedIntervalsAccumulate() = runTest {
        clock.currentMillis = 0L
        val sessionId = repository.startSession()

        clock.currentMillis = 1_000L
        repository.pauseSession(sessionId)
        clock.currentMillis = 3_000L // paused for 2000ms
        repository.resumeSession(sessionId)

        clock.currentMillis = 6_000L
        repository.pauseSession(sessionId)
        clock.currentMillis = 9_000L // paused for another 3000ms
        repository.resumeSession(sessionId)

        clock.currentMillis = 10_000L
        val state = repository.observeActiveSession().first()

        // elapsed = now(10000) - start(0) - pausedDuration(2000+3000=5000) = 5000
        assertEquals(5_000L, state?.elapsedDurationMillis)
    }

    @Test
    fun givenSessionNotPaused_whenResumeSession_thenNoPausedDurationAdded() = runTest {
        clock.currentMillis = 0L
        val sessionId = repository.startSession()

        clock.currentMillis = 4_000L
        repository.resumeSession(sessionId) // resume without ever pausing: no-op on paused duration

        clock.currentMillis = 10_000L
        val state = repository.observeActiveSession().first()

        assertEquals(10_000L, state?.elapsedDurationMillis)
    }

    @Test
    fun givenRecordedPoints_whenObserveActiveSession_thenDistanceAndSpeedsDerivedFromPoints() = runTest {
        clock.currentMillis = 0L
        val sessionId = repository.startSession()

        repository.recordLocationPoint(
            LocationPoint(sessionId, 10.7626, 106.6602, 1_000L, 5f, speedMetersPerSec = 2f),
        )
        repository.recordLocationPoint(
            LocationPoint(sessionId, 10.7630, 106.6602, 2_000L, 5f, speedMetersPerSec = 4f),
        )
        clock.currentMillis = 10_000L

        val state = repository.observeActiveSession().first()

        assertTrue(state!!.distanceMeters > 0.0)
        assertEquals(4f, state.currentSpeedMps, 0.0001f)
        // Derived from the route over 10 s, not the mean of the two samples (which would be 3 m/s).
        assertEquals(10_000L, state.elapsedDurationMillis)
        assertEquals((state.distanceMeters / 10.0).toFloat(), state.averageSpeedMps, 0.0001f)
        assertEquals(2, state.route.size)
    }

    @Test
    fun givenKnownDistanceAndDuration_whenStop_thenAverageSpeedIsDistanceOverMovingTime() = runTest {
        clock.currentMillis = 0L
        val sessionId = repository.startSession()
        clock.currentMillis = 100_000L

        // Average speed is not a parameter at all: it is derived from the distance and duration
        // actually persisted, so the caller cannot make the three numbers disagree.
        val summary = repository.stopSession(sessionId, null, finalDistanceMeters = 500.0)

        // 500 m over 100 s = 5 m/s.
        assertEquals(5f, summary.averageSpeedMps, 0.0001f)
    }

    @Test
    fun givenPausedInterval_whenStop_thenAverageSpeedExcludesPausedTime() = runTest {
        clock.currentMillis = 0L
        val sessionId = repository.startSession()
        clock.currentMillis = 40_000L
        repository.pauseSession(sessionId)
        clock.currentMillis = 90_000L
        repository.resumeSession(sessionId)
        clock.currentMillis = 100_000L

        val summary = repository.stopSession(sessionId, null, finalDistanceMeters = 500.0)

        // Moving time is 100 s minus the 50 s pause = 50 s, so 500 m / 50 s = 10 m/s.
        assertEquals(50_000L, summary.durationMillis)
        assertEquals(10f, summary.averageSpeedMps, 0.0001f)
    }

    @Test
    fun givenZeroMovingTime_whenStop_thenAverageSpeedIsZeroRatherThanInfinite() = runTest {
        clock.currentMillis = 5_000L
        val sessionId = repository.startSession()

        val summary = repository.stopSession(sessionId, null, finalDistanceMeters = 10.0)

        assertEquals(0f, summary.averageSpeedMps, 0.0001f)
    }

    @Test
    fun givenStoppedSession_whenReadBackFromHistory_thenAverageSpeedAgreesWithDistanceAndDuration() = runTest {
        clock.currentMillis = 0L
        val sessionId = repository.startSession()
        clock.currentMillis = 100_000L
        repository.stopSession(sessionId, null, finalDistanceMeters = 500.0)

        val stored = repository.observeSessionSummaries().first().single()

        assertEquals(500.0, stored.distanceMeters, 0.0001)
        assertEquals(100_000L, stored.durationMillis)
        assertEquals(5f, stored.averageSpeedMps, 0.0001f)
    }

    @Test
    fun givenStationaryJitterPoints_whenObserveActiveSession_thenDistanceExcludesDrift() = runTest {
        clock.currentMillis = 0L
        val sessionId = repository.startSession()
        // A phone sitting still: sub-metre wobble arriving every second.
        repeat(6) { index ->
            repository.recordLocationPoint(
                LocationPoint(
                    sessionId = sessionId,
                    latitude = 10.7626 + 0.0000045 * index,
                    longitude = 106.6602,
                    timestamp = 1_000L + index * 1_000L,
                    horizontalAccuracyMeters = 5f,
                    speedMetersPerSec = 0.1f,
                ),
            )
        }

        val state = repository.observeActiveSession().first()

        assertEquals(0.0, state!!.distanceMeters, 0.0001)
        assertEquals(6, state.route.size)
    }

    @Test
    fun givenSessionAlreadyStopped_whenLatePauseArrives_thenSessionStaysStopped() = runTest {
        // The real sequence: a Pause coroutine reads status == RUNNING, a Stop lands while it is
        // still suspended, and the Pause write arrives afterwards. It must not resurrect the row.
        val sessionId = repository.startSession()
        clock.currentMillis = 10_000L
        repository.stopSession(sessionId, "/thumb.png", finalDistanceMeters = 500.0)

        repository.pauseSession(sessionId)

        assertNull("A stopped session must never come back as the active session", repository.getActiveSessionId())
        assertEquals(SessionStatus.STOPPED, sessionDao.getById(sessionId)!!.status.let(SessionStatus::valueOf))
    }

    @Test
    fun givenSessionAlreadyStopped_whenLateResumeArrives_thenSessionStaysStopped() = runTest {
        val sessionId = repository.startSession()
        clock.currentMillis = 10_000L
        repository.stopSession(sessionId, null, finalDistanceMeters = 500.0)

        repository.resumeSession(sessionId)

        assertNull(repository.getActiveSessionId())
    }

    @Test
    fun givenSessionAlreadyStopped_whenLatePauseArrives_thenFinalStatsAreNotOverwritten() = runTest {
        val sessionId = repository.startSession()
        clock.currentMillis = 10_000L
        repository.stopSession(sessionId, "/thumb.png", finalDistanceMeters = 500.0)

        clock.currentMillis = 99_000L
        repository.pauseSession(sessionId)

        val row = sessionDao.getById(sessionId)!!
        assertEquals(10_000L, row.stoppedTimestamp)
        assertEquals(500.0, row.finalDistanceMeters!!, 0.0001)
        assertNull("A stopped session must not carry an in-progress pause marker", row.pausedAtTimestamp)
    }

    @Test
    fun givenPauseAndStopDispatchedConcurrently_whenBothComplete_thenTerminalStateIsStopped() = runTest {
        val sessionId = repository.startSession()
        clock.currentMillis = 10_000L

        val pause = launch { repository.pauseSession(sessionId) }
        val stop = launch { repository.stopSession(sessionId, null, finalDistanceMeters = 1.0) }
        pause.join()
        stop.join()

        assertEquals(SessionStatus.STOPPED, sessionDao.getById(sessionId)!!.status.let(SessionStatus::valueOf))
        assertNull(repository.getActiveSessionId())
    }

    @Test
    fun givenMoreThanOneActiveSessionRow_whenObserveActiveSession_thenMostRecentlyStartedWins() = runTest {
        clock.currentMillis = 1_000L
        val older = repository.startSession()
        sessionDao.updateStatusIfCurrent(
            sessionId = older,
            expectedCurrentStatus = SessionStatus.RUNNING.name,
            status = SessionStatus.PAUSED.name,
            pausedDurationMillis = 0L,
            pausedAtTimestamp = 1_000L,
        )
        clock.currentMillis = 5_000L
        val newer = repository.startSession()

        assertEquals(newer, repository.getActiveSessionId())
        assertEquals(newer, repository.observeActiveSession().first()!!.session.id)
    }

    @Test
    fun givenNoRecordedPoints_whenObserveActiveSession_thenSpeedsAndDistanceAreZero() = runTest {
        val sessionId = repository.startSession()

        val state = repository.observeActiveSession().first()

        assertEquals(sessionId, state?.session?.id)
        assertEquals(0.0, state?.distanceMeters)
        assertEquals(0f, state?.currentSpeedMps)
        assertEquals(0f, state?.averageSpeedMps)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun givenNoNewGpsFix_whenObserveActiveSession_thenTickerCausesElapsedDurationToAdvanceOverTime() = runTest {
        repository.startSession()

        val emissions = mutableListOf<Long?>()
        val job = launch { repository.observeActiveSession().collect { emissions.add(it?.elapsedDurationMillis) } }
        runCurrent()

        clock.currentMillis = 1_000L
        advanceTimeBy(1_001L)
        runCurrent()

        clock.currentMillis = 2_000L
        advanceTimeBy(1_000L)
        runCurrent()

        job.cancel()

        assertTrue("Expected at least 3 emissions from the ticker, got ${emissions.size}", emissions.size >= 3)
        assertEquals(listOf(0L, 1_000L, 2_000L), emissions.take(3))
    }

    @Test
    fun givenSessionStopped_whenStopSessionCalled_thenPersistsFinalStatsAndReturnsSummary() = runTest {
        clock.currentMillis = 0L
        val sessionId = repository.startSession()

        clock.currentMillis = 10_000L
        val summary = repository.stopSession(
            sessionId = sessionId,
            thumbnailPath = "/thumb.png",
            finalDistanceMeters = 500.0,
        )

        assertEquals(sessionId, summary.id)
        assertEquals(500.0, summary.distanceMeters, 0.0001)
        // 500 m over the 10 s between start and stop.
        assertEquals(50f, summary.averageSpeedMps, 0.0001f)
        assertEquals("/thumb.png", summary.thumbnailPath)
        assertEquals(10_000L, summary.durationMillis)

        assertNull(repository.getActiveSessionId())
        val summaries = repository.observeSessionSummaries().first()
        assertEquals(1, summaries.size)
        assertEquals(summary, summaries.first())
    }

    @Test
    fun givenSessionStoppedAfterPause_whenStopSessionCalled_thenDurationExcludesPausedTime() = runTest {
        clock.currentMillis = 0L
        val sessionId = repository.startSession()

        clock.currentMillis = 2_000L
        repository.pauseSession(sessionId)
        clock.currentMillis = 5_000L // paused for 3000ms
        repository.resumeSession(sessionId)

        clock.currentMillis = 10_000L
        val summary = repository.stopSession(sessionId, null, 0.0)

        // duration = 10000 - 0 - 3000 = 7000
        assertEquals(7_000L, summary.durationMillis)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun givenSessionPaused_whenObserveActiveSessionTicksWhileStillPaused_thenElapsedDurationStaysFrozen() = runTest {
        clock.currentMillis = 0L
        val sessionId = repository.startSession()

        clock.currentMillis = 5_000L
        repository.pauseSession(sessionId)

        val emissions = mutableListOf<Long?>()
        val job = launch { repository.observeActiveSession().collect { emissions.add(it?.elapsedDurationMillis) } }
        runCurrent()

        clock.currentMillis = 7_000L
        advanceTimeBy(1_001L)
        runCurrent()

        clock.currentMillis = 10_000L
        advanceTimeBy(3_000L)
        runCurrent()

        job.cancel()

        assertTrue(
            "Expected at least 3 emissions from the ticker, got ${emissions.size}",
            emissions.size >= 3,
        )
        // Frozen at pause point: elapsed = pausedAt(5000) - start(0) - pausedDuration(0) = 5000,
        // for every ticker emission while still paused, regardless of wall-clock advancement.
        assertEquals(listOf(5_000L, 5_000L, 5_000L), emissions.take(3))
    }

    @Test
    fun givenSessionPausedAndNeverResumed_whenStopSessionCalled_thenDurationExcludesOngoingPauseInterval() = runTest {
        clock.currentMillis = 0L
        val sessionId = repository.startSession()

        clock.currentMillis = 5_000L
        repository.pauseSession(sessionId)

        clock.currentMillis = 8_000L
        val summary = repository.stopSession(sessionId, null, 0.0)

        // duration = pausedAt(5000) - start(0) - pausedDuration(0) = 5000, NOT 8000.
        assertEquals(5_000L, summary.durationMillis)
    }

    @Test
    fun givenSessionPausedAndNeverResumed_whenReadBackViaObserveSessionSummaries_thenDurationExcludesOngoingPauseInterval() =
        runTest {
            clock.currentMillis = 0L
            val sessionId = repository.startSession()

            clock.currentMillis = 5_000L
            repository.pauseSession(sessionId)

            clock.currentMillis = 8_000L
            repository.stopSession(sessionId, null, 0.0)

            val summaries = repository.observeSessionSummaries().first()

            assertEquals(1, summaries.size)
            // duration = stopped(8000) - start(0) - pausedDuration(3000, folded in at stop time) = 5000, NOT 8000.
            assertEquals(5_000L, summaries.first().durationMillis)
        }

    @Test
    fun givenPointRecorded_whenGetMostRecentPointAndGetPointsForSession_thenDelegatesToDao() = runTest {
        val sessionId = repository.startSession()
        val point = LocationPoint(sessionId, 1.0, 2.0, 1_000L, 5f, 3f)

        repository.recordLocationPoint(point)

        assertEquals(point, repository.getMostRecentPoint(sessionId))
        assertEquals(listOf(point), repository.getPointsForSession(sessionId))
    }

    @Test
    fun givenNoPointsRecorded_whenGetMostRecentPoint_thenReturnsNull() = runTest {
        val sessionId = repository.startSession()

        assertNull(repository.getMostRecentPoint(sessionId))
    }
}
