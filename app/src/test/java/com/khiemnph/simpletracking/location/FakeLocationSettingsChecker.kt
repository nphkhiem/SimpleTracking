package com.khiemnph.simpletracking.location

/** Test-only [LocationSettingsChecker] whose [check] result a test configures directly, instead
 * of orchestrating real Play Services Task callbacks through [RecordFragmentTest][com.khiemnph.simpletracking.ui.record.RecordFragmentTest]. */
class FakeLocationSettingsChecker : LocationSettingsChecker {

    var result: LocationSettingsResult = LocationSettingsResult.Satisfied

    override suspend fun check(): LocationSettingsResult = result
}
