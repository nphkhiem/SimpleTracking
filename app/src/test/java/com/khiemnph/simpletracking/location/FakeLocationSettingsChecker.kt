package com.khiemnph.simpletracking.location

/** Test-only [LocationSettingsChecker] whose [check] result a test configures directly, instead
 * of orchestrating real Play Services Task callbacks through [RecordFragmentTest][com.khiemnph.simpletracking.ui.record.RecordFragmentTest].
 * [checkCallCount] lets a test assert whether [check] ran at all - e.g. proving a guard skipped
 * re-checking rather than merely that its outcome happened to be a no-op. */
class FakeLocationSettingsChecker : LocationSettingsChecker {

    var result: LocationSettingsResult = LocationSettingsResult.Satisfied
    var checkCallCount: Int = 0
        private set

    override suspend fun check(): LocationSettingsResult {
        checkCallCount++
        return result
    }
}
