package com.khiemnph.simpletracking.ui.record

import com.khiemnph.simpletracking.ui.runs.RunsFragmentDirections
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression coverage for widening [RecordFragmentArgs.sessionId] from a required to a nullable
 * Safe Args argument: the new "start a brand-new session" navigation case from
 * [com.khiemnph.simpletracking.ui.runs.RunsFragment] has no sessionId yet, while
 * [com.khiemnph.simpletracking.ui.MainActivity]'s existing "resume an active session" case
 * (covered by `MainActivityTest`) always passes a concrete one - both must keep working.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecordFragmentArgsTest {

    @Test
    fun givenNullSessionId_whenArgsRoundTripThroughABundle_thenSessionIdRemainsNull() {
        val args = RecordFragmentArgs(sessionId = null)

        val roundTripped = RecordFragmentArgs.fromBundle(args.toBundle())

        assertNull(roundTripped.sessionId)
    }

    @Test
    fun givenNonNullSessionId_whenArgsRoundTripThroughABundle_thenSessionIdIsPreserved() {
        val args = RecordFragmentArgs(sessionId = "session-42")

        val roundTripped = RecordFragmentArgs.fromBundle(args.toBundle())

        assertEquals("session-42", roundTripped.sessionId)
    }

    @Test
    fun givenNoSessionIdArgumentInTheBundle_whenArgsReadFromBundle_thenSessionIdDefaultsToNull() {
        val emptyArgs = RecordFragmentArgs()

        assertNull(RecordFragmentArgs.fromBundle(emptyArgs.toBundle()).sessionId)
    }

    @Test
    fun givenRunsFragmentDirectionsCalledWithNullSessionId_whenArgumentsBuilt_thenBundleHasNullSessionId() {
        val direction = RunsFragmentDirections.actionRunsFragmentToRecordFragment(sessionId = null)

        assertNull(RecordFragmentArgs.fromBundle(direction.arguments).sessionId)
    }

    @Test
    fun givenRunsFragmentDirectionsCalledWithNoArguments_whenArgumentsBuilt_thenSessionIdDefaultsToNull() {
        val direction = RunsFragmentDirections.actionRunsFragmentToRecordFragment()

        assertNull(RecordFragmentArgs.fromBundle(direction.arguments).sessionId)
    }

    @Test
    fun givenRunsFragmentDirectionsCalledWithAConcreteSessionId_whenArgumentsBuilt_thenBundleHasThatSessionId() {
        val direction = RunsFragmentDirections.actionRunsFragmentToRecordFragment(sessionId = "session-99")

        assertEquals("session-99", RecordFragmentArgs.fromBundle(direction.arguments).sessionId)
    }
}
