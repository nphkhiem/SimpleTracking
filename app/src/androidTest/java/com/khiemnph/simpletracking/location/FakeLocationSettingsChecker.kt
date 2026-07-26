package com.khiemnph.simpletracking.location

import com.khiemnph.data.location.LocationSettingsChecker
import com.khiemnph.data.location.LocationSettingsResult

/**
 * Test-only [LocationSettingsChecker] whose [check] result a test configures directly, instead of
 * orchestrating real Play Services Task callbacks through a real device's Location Service state.
 * Mirrors the JVM-side `app/src/test` fake of the same name - that one isn't visible to
 * `app/src/androidTest` (separate Gradle source sets), so this is a deliberate duplicate rather
 * than a shared dependency.
 */
class FakeLocationSettingsChecker : LocationSettingsChecker {

    var result: LocationSettingsResult = LocationSettingsResult.Satisfied

    override suspend fun check(): LocationSettingsResult = result
}
