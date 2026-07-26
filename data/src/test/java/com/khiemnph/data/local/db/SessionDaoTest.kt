package com.khiemnph.data.local.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.util.concurrent.Executor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SessionDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: SessionDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(Executor { it.run() })
            .setTransactionExecutor(Executor { it.run() })
            .build()
        dao = database.sessionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun givenSessionInserted_whenObserveActiveSession_thenEmitsThatSession() = runTest {
        val entity = sessionEntity(id = "s1", status = "RUNNING")
        dao.upsert(entity)

        val result = dao.observeActiveSession().first()

        assertEquals(entity, result)
    }

    @Test
    fun givenNoSessionInserted_whenObserveActiveSession_thenEmitsNull() = runTest {
        assertNull(dao.observeActiveSession().first())
    }

    @Test
    fun givenSessionAlreadyStopped_whenObserveActiveSession_thenEmitsNull() = runTest {
        dao.upsert(sessionEntity(id = "s1", status = "STOPPED", stoppedTimestamp = 1_000L))

        assertNull(dao.observeActiveSession().first())
    }

    @Test
    fun givenPausedSession_whenObserveActiveSession_thenStillEmitsIt() = runTest {
        val entity = sessionEntity(id = "s1", status = "PAUSED", pausedAtTimestamp = 500L)
        dao.upsert(entity)

        val result = dao.observeActiveSession().first()

        assertEquals(entity, result)
    }

    @Test
    fun givenUpdateStatus_whenGetById_thenReflectsNewStatusAndPausedFields() = runTest {
        dao.upsert(sessionEntity(id = "s1", status = "RUNNING"))

        dao.updateStatusIfCurrent(
            sessionId = "s1",
            expectedCurrentStatus = "RUNNING",
            status = "PAUSED",
            pausedDurationMillis = 0L,
            pausedAtTimestamp = 1_000L,
            pausedAtElapsedRealtimeMillis = null,
        )

        val updated = dao.getById("s1")
        assertEquals("PAUSED", updated?.status)
        assertEquals(1_000L, updated?.pausedAtTimestamp)
    }

    @Test
    fun givenStatusNoLongerMatchesExpected_whenUpdateStatusIfCurrent_thenNoRowsChanged() = runTest {
        dao.upsert(sessionEntity(id = "s1", status = "STOPPED"))

        val updated = dao.updateStatusIfCurrent(
            sessionId = "s1",
            expectedCurrentStatus = "RUNNING",
            status = "PAUSED",
            pausedDurationMillis = 0L,
            pausedAtTimestamp = 1_000L,
            pausedAtElapsedRealtimeMillis = null,
        )

        assertEquals(0, updated)
        assertEquals("STOPPED", dao.getById("s1")?.status)
    }

    @Test
    fun givenSessionAlreadyStopped_whenWriteFinalStats_thenNoRowsChanged() = runTest {
        dao.upsert(sessionEntity(id = "s1", status = "STOPPED"))

        val updated = dao.writeFinalStats(
            sessionId = "s1",
            status = "STOPPED",
            stoppedTimestamp = 9_000L,
            pausedDurationMillis = 0L,
            finalDistanceMeters = 1.0,
            finalAverageSpeedMps = 1f,
            routePolyline = null,
        )

        assertEquals(0, updated)
    }

    @Test
    fun givenWriteFinalStats_whenObserveSummaries_thenIncludesStoppedSessionWithFinalStats() = runTest {
        dao.upsert(sessionEntity(id = "s1", status = "RUNNING"))

        dao.writeFinalStats(
            sessionId = "s1",
            status = "STOPPED",
            stoppedTimestamp = 5_000L,
            pausedDurationMillis = 1_500L,
            finalDistanceMeters = 123.4,
            finalAverageSpeedMps = 2.5f,
            routePolyline = "/path/thumb.png",
        )

        val summaries = dao.observeSummaries().first()
        assertEquals(1, summaries.size)
        assertEquals("STOPPED", summaries.first().status)
        assertEquals(123.4, summaries.first().finalDistanceMeters)
        assertEquals(2.5f, summaries.first().finalAverageSpeedMps)
        assertEquals("/path/thumb.png", summaries.first().routePolyline)
        assertEquals(1_500L, summaries.first().pausedDurationMillis)
    }

    @Test
    fun givenSessionPaused_whenWriteFinalStats_thenClearsPausedAtTimestamp() = runTest {
        dao.upsert(sessionEntity(id = "s1", status = "PAUSED", pausedAtTimestamp = 2_000L))

        dao.writeFinalStats(
            sessionId = "s1",
            status = "STOPPED",
            stoppedTimestamp = 5_000L,
            pausedDurationMillis = 2_000L,
            finalDistanceMeters = 0.0,
            finalAverageSpeedMps = 0f,
            routePolyline = null,
        )

        val updated = dao.getById("s1")
        assertNull(updated?.pausedAtTimestamp)
    }

    @Test
    fun givenMultipleStoppedSessions_whenObserveSummaries_thenOrderedByStoppedTimestampDescending() = runTest {
        dao.upsert(sessionEntity(id = "older", status = "STOPPED", stoppedTimestamp = 1_000L))
        dao.upsert(sessionEntity(id = "newer", status = "STOPPED", stoppedTimestamp = 5_000L))

        val summaries = dao.observeSummaries().first()

        assertEquals(listOf("newer", "older"), summaries.map { it.id })
    }

    @Test
    fun givenRunningSession_whenGetActiveSessionId_thenReturnsItsId() = runTest {
        dao.upsert(sessionEntity(id = "s1", status = "RUNNING"))

        assertEquals("s1", dao.getActiveSessionId())
    }

    @Test
    fun givenNoActiveSession_whenGetActiveSessionId_thenReturnsNull() = runTest {
        dao.upsert(sessionEntity(id = "s1", status = "STOPPED", stoppedTimestamp = 1_000L))

        assertNull(dao.getActiveSessionId())
    }
}
