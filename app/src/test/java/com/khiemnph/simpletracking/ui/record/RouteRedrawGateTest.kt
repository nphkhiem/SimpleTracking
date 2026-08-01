package com.khiemnph.simpletracking.ui.record

import com.khiemnph.domain.model.LatLngPoint
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteRedrawGateTest {

    private val gate = RouteRedrawGate()

    private fun route(count: Int, offset: Double = 0.0) =
        List(count) { LatLngPoint(21.0 + it * 0.001 + offset, 105.8 + it * 0.001) }

    @Test
    fun `the first route is drawn`() {
        assertTrue(gate.needsRedraw(route(3)))
    }

    @Test
    fun `an empty first route is drawn, so a stale overlay is cleared`() {
        assertTrue(gate.needsRedraw(emptyList()))
    }

    @Test
    fun `the same route is not drawn twice`() {
        gate.needsRedraw(route(3))

        assertFalse(gate.needsRedraw(route(3)))
    }

    @Test
    fun `a ticker re-emission with no new fix is not drawn`() {
        // The repository re-emits once a second to advance the elapsed duration, carrying a route
        // that has not changed. That emission is the whole reason this gate exists.
        val unchanged = route(40)
        gate.needsRedraw(unchanged)

        repeat(10) { assertFalse(gate.needsRedraw(unchanged)) }
    }

    @Test
    fun `an appended fix is drawn`() {
        gate.needsRedraw(route(3))

        assertTrue(gate.needsRedraw(route(4)))
    }

    @Test
    fun `a different route of the same length is drawn`() {
        // Starting a second session can hand over a route with the same point count as the last
        // one drawn. Gating on count alone would leave the previous session's line on the map.
        gate.needsRedraw(route(3))

        assertTrue(gate.needsRedraw(route(3, offset = 5.0)))
    }

    @Test
    fun `clearing to empty is drawn, so the previous session's line goes`() {
        gate.needsRedraw(route(3))

        assertTrue(gate.needsRedraw(emptyList()))
    }

    @Test
    fun `an unchanged empty route is not drawn twice`() {
        gate.needsRedraw(emptyList())

        assertFalse(gate.needsRedraw(emptyList()))
    }

    @Test
    fun `invalidating forces the next route to be drawn`() {
        // A freshly bound GoogleMap holds none of what was drawn on the previous one, so the gate
        // has to forget what it thinks is on screen.
        val unchanged = route(3)
        gate.needsRedraw(unchanged)

        gate.invalidate()

        assertTrue(gate.needsRedraw(unchanged))
    }

    @Test
    fun `invalidating twice still only forces one redraw`() {
        val unchanged = route(3)
        gate.invalidate()
        gate.invalidate()
        assertTrue(gate.needsRedraw(unchanged))

        assertFalse(gate.needsRedraw(unchanged))
    }
}
