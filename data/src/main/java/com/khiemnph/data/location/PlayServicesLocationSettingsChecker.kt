package com.khiemnph.data.location

import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.SettingsClient
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** Runs the completion listener on whichever thread the Task actually completes on, rather than
 * posting it to the main thread's Looper - this suspend function's caller shouldn't need a live,
 * unblocked main Looper to ever resume. */
private val directExecutor = Executor { command -> command.run() }

/**
 * Wraps [SettingsClient.checkLocationSettings] as a suspend function. The
 * [LocationSettingsRequest] is built from [buildTrackingLocationRequest] - the exact same request
 * shape [com.khiemnph.data.location.FusedLocationTrackingRepository] uses to actually collect
 * fixes - so this check reflects what the app will really ask the device for.
 */
class PlayServicesLocationSettingsChecker @Inject constructor(
    private val settingsClient: SettingsClient,
) : LocationSettingsChecker {

    override suspend fun check(): LocationSettingsResult = suspendCancellableCoroutine { continuation ->
        val request = LocationSettingsRequest.Builder()
            .addLocationRequest(buildTrackingLocationRequest())
            .build()
        settingsClient.checkLocationSettings(request)
            .addOnSuccessListener(directExecutor) { continuation.resume(LocationSettingsResult.Satisfied) }
            .addOnFailureListener(directExecutor) { exception ->
                val result = if (exception is ResolvableApiException) {
                    LocationSettingsResult.ResolutionRequired(exception.resolution.intentSender)
                } else {
                    LocationSettingsResult.Unresolvable
                }
                continuation.resume(result)
            }
    }
}
