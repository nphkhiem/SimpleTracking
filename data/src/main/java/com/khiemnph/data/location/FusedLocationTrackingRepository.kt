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
import com.khiemnph.domain.repository.SessionRepository
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/**
 * Maps raw `FusedLocationProviderClient` updates to domain [RawLocationFix]es. Deliberately does
 * NOT validate fixes or resolve missing speed — that's [com.khiemnph.domain.interactor.RecordLocationFixUseCase]'s
 * job. `android.location.Location` and `com.google.android.gms.location.*` types never leak past
 * this class.
 *
 * Each fix is tagged with whatever session is currently active per [sessionRepository]. A fix
 * arriving with no active session (e.g. a stray update after tracking stopped, before the OS has
 * torn down the callback) is dropped rather than emitted with a bogus session id — there is
 * nothing meaningful to attach it to.
 */
class FusedLocationTrackingRepository @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient,
    private val sessionRepository: SessionRepository,
) : LocationTrackingRepository {

    @SuppressLint("MissingPermission")
    override fun locationUpdates(): Flow<RawLocationFix> = callbackFlow {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    launch {
                        val sessionId = sessionRepository.getActiveSessionId() ?: return@launch
                        trySend(location.toRawLocationFix(sessionId))
                    }
                }
            }
        }
        val request = LocationRequest.Builder(LOCATION_UPDATE_INTERVAL_MILLIS)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()
        fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { fusedLocationClient.removeLocationUpdates(callback) }
    }

    private companion object {
        const val LOCATION_UPDATE_INTERVAL_MILLIS = 2_000L
    }
}

internal fun Location.toRawLocationFix(sessionId: String): RawLocationFix = RawLocationFix(
    sessionId = sessionId,
    latitude = latitude,
    longitude = longitude,
    timestamp = time,
    horizontalAccuracyMeters = accuracy,
    speedMetersPerSec = if (hasSpeed()) speed else null,
)
