package com.khiemnph.simpletracking.ui.record

import com.khiemnph.domain.model.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rule that keeps the Pause/Resume button honest between a tap and the state emission that
 * confirms it.
 *
 * A tap travels intent to service to coroutine to Room write to invalidation to emission, which
 * takes roughly a second. Deciding the next command from the last emitted status during that
 * window makes the button feel dead and re-sends the command already in flight.
 */
class PauseResumeGateTest {

    private val gate = PauseResumeGate()

    @Test
    fun `a tap while running targets paused`() {
        assertEquals(SessionStatus.PAUSED, gate.onTapped(SessionStatus.RUNNING))
    }

    @Test
    fun `a tap while paused targets running`() {
        assertEquals(SessionStatus.RUNNING, gate.onTapped(SessionStatus.PAUSED))
    }

    @Test
    fun `a second tap inside the window targets the opposite of the first, not a repeat`() {
        gate.onTapped(SessionStatus.RUNNING)

        // The user has already seen the button flip to Paused, so tapping again means Resume.
        assertEquals(SessionStatus.RUNNING, gate.onTapped(SessionStatus.RUNNING))
    }

    @Test
    fun `displays the pending status while the real one has not caught up`() {
        gate.onTapped(SessionStatus.RUNNING)

        assertEquals(SessionStatus.PAUSED, gate.displayStatus(SessionStatus.RUNNING))
    }

    @Test
    fun `stops overriding once the real status matches what was asked for`() {
        gate.onTapped(SessionStatus.RUNNING)

        assertEquals(SessionStatus.PAUSED, gate.displayStatus(SessionStatus.PAUSED))
        assertFalse(gate.isPending)
        assertEquals(SessionStatus.RUNNING, gate.displayStatus(SessionStatus.RUNNING))
    }

    @Test
    fun `passes the real status straight through when nothing is pending`() {
        assertEquals(SessionStatus.RUNNING, gate.displayStatus(SessionStatus.RUNNING))
        assertEquals(SessionStatus.STOPPED, gate.displayStatus(SessionStatus.STOPPED))
    }

    @Test
    fun `a stop overrides a pending pause rather than being masked by it`() {
        gate.onTapped(SessionStatus.RUNNING)

        // Stopping is terminal: it must never be hidden behind a pause the user asked for.
        assertEquals(SessionStatus.STOPPED, gate.displayStatus(SessionStatus.STOPPED))
        assertFalse(gate.isPending)
    }

    @Test
    fun `gives up after the command has had long enough to land`() {
        gate.onTapped(SessionStatus.RUNNING)
        assertTrue(gate.isPending)

        gate.expireIfStale(elapsedSinceTapMillis = PauseResumeGate.PENDING_TIMEOUT_MILLIS)

        assertFalse("a command that never landed must not freeze the button", gate.isPending)
        assertEquals(SessionStatus.RUNNING, gate.displayStatus(SessionStatus.RUNNING))
    }

    @Test
    fun `keeps waiting while still inside the window`() {
        gate.onTapped(SessionStatus.RUNNING)

        gate.expireIfStale(elapsedSinceTapMillis = PauseResumeGate.PENDING_TIMEOUT_MILLIS - 1)

        assertTrue(gate.isPending)
    }
}
