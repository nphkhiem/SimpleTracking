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

    /** The Location Service isn't satisfied and there's no system dialog that can fix it. */
    data object Unresolvable : LocationSettingsResult
}
