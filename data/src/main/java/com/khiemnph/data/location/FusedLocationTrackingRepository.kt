package com.khiemnph.data.location

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.khiemnph.domain.model.RawLocationFix
import com.khiemnph.domain.repository.LocationTrackingRepository
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Maps raw `FusedLocationProviderClient` updates to domain [RawLocationFix]es. Deliberately does
 * NOT validate fixes or resolve missing speed — that's [com.khiemnph.domain.interactor.RecordLocationFixUseCase]'s
 * job. `android.location.Location` and `com.google.android.gms.location.*` types never leak past
 * this class.
 *
 * Each fix is tagged with the [sessionId] the caller passes to [locationUpdates] — this class has
 * no notion of session lifecycle of its own.
 */
class FusedLocationTrackingRepository @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient,
) : LocationTrackingRepository {

    @SuppressLint("MissingPermission")
    override fun locationUpdates(sessionId: String): Flow<RawLocationFix> = callbackFlow {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    trySend(location.toRawLocationFix(sessionId))
                }
            }
        }
        fusedLocationClient.requestLocationUpdates(buildTrackingLocationRequest(), callback, Looper.getMainLooper())
        awaitClose { fusedLocationClient.removeLocationUpdates(callback) }
    }
}

private const val LOCATION_UPDATE_INTERVAL_MILLIS = 2_000L
private const val NANOS_PER_MILLI = 1_000_000L

/**
 * The exact request shape this app tracks a session with - shared with
 * `com.khiemnph.simpletracking.location.LocationSettingsChecker` so its device Location-Service
 * check reflects what will actually be requested, rather than an independently-maintained
 * approximation of it.
 */
internal fun buildTrackingLocationRequest(): LocationRequest = LocationRequest.Builder(LOCATION_UPDATE_INTERVAL_MILLIS)
    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
    .build()

internal fun Location.toRawLocationFix(sessionId: String): RawLocationFix = RawLocationFix(
    sessionId = sessionId,
    latitude = latitude,
    longitude = longitude,
    timestamp = time,
    // Nanos since boot, from the fix itself rather than from a clock read on arrival, so queueing
    // between the provider and here cannot stretch an interval.
    elapsedRealtimeMillis = elapsedRealtimeNanos / NANOS_PER_MILLI,
    horizontalAccuracyMeters = accuracy,
    speedMetersPerSec = if (hasSpeed()) speed else null,
)
