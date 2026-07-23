package com.khiemnph.data.local.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
class LocationPointDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var sessionDao: SessionDao
    private lateinit var pointDao: LocationPointDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sessionDao = database.sessionDao()
        pointDao = database.locationPointDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun seedSession(sessionId: String) {
        sessionDao.upsert(sessionEntity(id = sessionId))
    }

    @Test
    fun givenPointsInsertedOutOfOrder_whenGetPointsForSession_thenReturnedOrderedByTimestamp() = runTest {
        seedSession("s1")
        pointDao.insert(locationPointEntity(sessionId = "s1", timestamp = 3_000L))
        pointDao.insert(locationPointEntity(sessionId = "s1", timestamp = 1_000L))
        pointDao.insert(locationPointEntity(sessionId = "s1", timestamp = 2_000L))

        val result = pointDao.getPointsForSession("s1")

        assertEquals(listOf(1_000L, 2_000L, 3_000L), result.map { it.timestamp })
    }

    @Test
    fun givenPointsInsertedOutOfOrder_whenObservePointsForSession_thenEmitsOrderedByTimestamp() = runTest {
        seedSession("s1")
        pointDao.insert(locationPointEntity(sessionId = "s1", timestamp = 2_000L))
        pointDao.insert(locationPointEntity(sessionId = "s1", timestamp = 1_000L))

        val result = pointDao.observePointsForSession("s1").first()

        assertEquals(listOf(1_000L, 2_000L), result.map { it.timestamp })
    }

    @Test
    fun givenPointsForDifferentSession_whenGetPointsForSession_thenOnlyMatchingSessionReturned() = runTest {
        seedSession("s1")
        seedSession("s2")
        pointDao.insert(locationPointEntity(sessionId = "s1", timestamp = 1_000L))
        pointDao.insert(locationPointEntity(sessionId = "s2", timestamp = 2_000L))

        val result = pointDao.getPointsForSession("s1")

        assertEquals(1, result.size)
        assertEquals("s1", result.first().sessionId)
    }

    @Test
    fun givenMultiplePointsInsertedOutOfTimestampOrder_whenGetMostRecentPoint_thenReturnsLatestTimestampNotLastInserted() =
        runTest {
            seedSession("s1")
            // Inserted last, but its timestamp is the earliest — getMostRecentPoint must not
            // just take the last-inserted row.
            pointDao.insert(locationPointEntity(sessionId = "s1", timestamp = 5_000L))
            pointDao.insert(locationPointEntity(sessionId = "s1", timestamp = 9_000L))
            pointDao.insert(locationPointEntity(sessionId = "s1", timestamp = 1_000L))

            val recent = pointDao.getMostRecentPoint("s1")

            assertEquals(9_000L, recent?.timestamp)
        }

    @Test
    fun givenNoPointsForSession_whenGetMostRecentPoint_thenReturnsNull() = runTest {
        seedSession("s1")

        assertNull(pointDao.getMostRecentPoint("s1"))
    }
}
