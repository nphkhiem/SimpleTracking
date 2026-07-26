package com.khiemnph.simpletracking.location

import android.content.IntentSender

/** Outcome of asking Play Services whether the device's Location Service (the GPS toggle, not the
 * app's own `ACCESS_FINE_LOCATION` permission) satisfies what this app needs to track a session. */
sealed interface LocationSettingsResult {

    /** The Location Service is already on and satisfies the app's tracking request. */
    data object Satisfied : LocationSettingsResult

    /** The Location Service can be turned on by launching [intentSender] via
     * `ActivityResultContracts.StartIntentSenderForResult`. */
    data class ResolutionRequired(val intentSender: IntentSender) : LocationSettingsResult

    /**
     * The check itself could not produce an answer, so the Location Service's state is unknown.
     *
     * This is NOT "location is off". Play Services returns a plain failure here for reasons that
     * have nothing to do with the GPS toggle - most commonly no network, since the check is a Play
     * Services call. Treating it as a definitive negative is why the app used to refuse to record
     * offline, and why a mid-run network blip could pause a session that was tracking perfectly.
     *
     * Callers should degrade rather than block: GPS is device hardware and works with no network.
     */
    data object Unresolvable : LocationSettingsResult
}
