package com.khiemnph.simpletracking.permission

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LocationPermissionAskTrackerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun givenNeverAsked_whenHasAskedBefore_thenReturnsFalse() {
        val tracker = LocationPermissionAskTracker(context)

        assertFalse(tracker.hasAskedBefore())
    }

    @Test
    fun givenMarkAskedCalled_whenHasAskedBefore_thenReturnsTrue() {
        val tracker = LocationPermissionAskTracker(context)

        tracker.markAsked()

        assertTrue(tracker.hasAskedBefore())
    }

    @Test
    fun givenMarkAskedPersistedByOneInstance_whenQueriedByANewInstance_thenStillReturnsTrue() {
        LocationPermissionAskTracker(context).markAsked()

        val secondInstance = LocationPermissionAskTracker(context)

        assertTrue(secondInstance.hasAskedBefore())
    }
}
