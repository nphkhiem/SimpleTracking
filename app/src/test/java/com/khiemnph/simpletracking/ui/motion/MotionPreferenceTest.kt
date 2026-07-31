package com.khiemnph.simpletracking.ui.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Whether to animate at all is a system preference, not the app's choice. Someone who turns
 * animations off in Developer options or in accessibility settings has asked every app to stop,
 * and motion sickness is the usual reason.
 */
class MotionPreferenceTest {

    @Test
    fun `animations run at the normal system scale`() {
        assertTrue(MotionPreference.animationsEnabled(durationScale = 1.0f))
    }

    @Test
    fun `animations run when the system has slowed them down`() {
        // A slowed scale is a debugging aid, not a request to stop.
        assertTrue(MotionPreference.animationsEnabled(durationScale = 5.0f))
    }

    @Test
    fun `a zero scale means animations are off system-wide`() {
        assertFalse(MotionPreference.animationsEnabled(durationScale = 0f))
    }

    @Test
    fun `a scale that could not be read is treated as animations on`() {
        // Settings.Global returns 0 both for "off" and for "unset" on some devices. Erring toward
        // animating is wrong; erring toward not animating would break the app for everyone whose
        // device reports nothing. The negative sentinel distinguishes the two.
        assertTrue(MotionPreference.animationsEnabled(durationScale = -1f))
    }

    @Test
    fun `duration is scaled by the system setting`() {
        assertEquals(300, MotionPreference.scaledDurationMillis(300, durationScale = 1.0f))
        assertEquals(600, MotionPreference.scaledDurationMillis(300, durationScale = 2.0f))
    }

    @Test
    fun `duration collapses to zero when animations are off`() {
        assertEquals(0, MotionPreference.scaledDurationMillis(300, durationScale = 0f))
    }

    @Test
    fun `an unreadable scale leaves the duration alone`() {
        assertEquals(300, MotionPreference.scaledDurationMillis(300, durationScale = -1f))
    }
}
