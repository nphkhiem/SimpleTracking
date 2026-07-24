package com.khiemnph.simpletracking.location

/**
 * Checks whether the device's Location Service (the GPS toggle, separate from the app's own
 * `ACCESS_FINE_LOCATION` permission) satisfies what this app needs before starting - or while
 * running - a tracking session.
 */
interface LocationSettingsChecker {
    suspend fun check(): LocationSettingsResult
}
