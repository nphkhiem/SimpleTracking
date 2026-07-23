package com.khiemnph.data.location

import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.khiemnph.domain.fake.MockedSessionRepository
import com.khiemnph.domain.model.RawLocationFix
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FusedLocationTrackingRepositoryTest {

    private fun locationOf(
        latitude: Double,
        longitude: Double,
        time: Long,
        accuracy: Float = 5f,
        speed: Float? = null,
    ): Location = Location("gps").apply {
        this.latitude = latitude
        this.longitude = longitude
        this.time = time
        this.accuracy = accuracy
        if (speed != null) this.speed = speed
    }

    @Test
    fun givenLocationWithSpeed_whenMappedToRawLocationFix_thenSpeedIsCarriedOverAsIs() {
        val location = locationOf(10.0, 20.0, 1_000L, accuracy = 5f, speed = 3.2f)

        val fix = location.toRawLocationFix("session-1")

        assertEquals("session-1", fix.sessionId)
        assertEquals(10.0, fix.latitude, 0.0)
        assertEquals(20.0, fix.longitude, 0.0)
        assertEquals(1_000L, fix.timestamp)
        assertEquals(5f, fix.horizontalAccuracyMeters, 0.0001f)
        assertEquals(3.2f, fix.speedMetersPerSec)
    }

    @Test
    fun givenLocationWithoutSpeed_whenMappedToRawLocationFix_thenSpeedIsNull() {
        val location = locationOf(10.0, 20.0, 1_000L, speed = null)

        val fix = location.toRawLocationFix("session-1")

        assertNull(fix.speedMetersPerSec)
    }

    @Test
    fun givenActiveSessionAndIncomingLocation_whenLocationUpdates_thenEmitsFixTaggedWithActiveSessionId() = runTest {
        val fusedLocationClient = mockk<FusedLocationProviderClient>(relaxed = true)
        val sessionRepository = MockedSessionRepository()
        val sessionId = sessionRepository.startSession()
        val callbackSlot = slot<LocationCallback>()
        every {
            fusedLocationClient.requestLocationUpdates(any<LocationRequest>(), capture(callbackSlot), any())
        } returns mockk(relaxed = true)
        val repository = FusedLocationTrackingRepository(fusedLocationClient, sessionRepository)

        val results = mutableListOf<RawLocationFix>()
        val collectJob = launch { repository.locationUpdates().toList(results) }
        // Let the flow builder run and register the callback before delivering an update.
        runCurrent()

        callbackSlot.captured.onLocationResult(LocationResult.create(listOf(locationOf(1.0, 2.0, 500L, speed = 4f))))
        runCurrent()
        runCurrent()

        collectJob.cancel()
        assertEquals(1, results.size)
        assertEquals(sessionId, results.first().sessionId)
        assertEquals(4f, results.first().speedMetersPerSec)
    }

    @Test
    fun givenNoActiveSession_whenLocationUpdates_thenFixIsDropped() = runTest {
        val fusedLocationClient = mockk<FusedLocationProviderClient>(relaxed = true)
        val sessionRepository = MockedSessionRepository()
        val callbackSlot = slot<LocationCallback>()
        every {
            fusedLocationClient.requestLocationUpdates(any<LocationRequest>(), capture(callbackSlot), any())
        } returns mockk(relaxed = true)
        val repository = FusedLocationTrackingRepository(fusedLocationClient, sessionRepository)

        val results = mutableListOf<RawLocationFix>()
        val collectJob = launch { repository.locationUpdates().toList(results) }
        runCurrent()

        callbackSlot.captured.onLocationResult(LocationResult.create(listOf(locationOf(1.0, 2.0, 500L))))
        runCurrent()
        runCurrent()

        collectJob.cancel()
        assertTrue(results.isEmpty())
    }
}
