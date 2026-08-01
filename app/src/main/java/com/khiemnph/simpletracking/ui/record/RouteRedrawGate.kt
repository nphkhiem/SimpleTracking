package com.khiemnph.simpletracking.ui.record

import com.khiemnph.domain.model.LatLngPoint

/**
 * Decides whether the map overlay actually has to be rebuilt.
 *
 * The repository re-emits the active session once a second so the elapsed duration advances,
 * whether or not a GPS fix arrived. Record rendered that emission straight through, and rebuilding
 * the overlay means `clear()`, a fresh polyline over every point so far, two markers and a camera
 * animation. Once a second, for the whole run, on the screen the app keeps open longest. On a
 * running app that is the wrong place to spend battery.
 *
 * Identity is the point count plus the newest point, not the count alone. Points are only ever
 * appended within a session, so the count settles it there, but starting a second session can hand
 * over a route with the same count as the one already drawn, and gating on count alone would leave
 * the previous session's line on the map.
 *
 * Deliberately a plain class with no Android types, following [PauseResumeGate]: the caller passes
 * the route in, which is what makes every rule here directly testable.
 */
class RouteRedrawGate {

    private var hasDrawn = false
    private var drawnCount = 0
    private var drawnNewest: LatLngPoint? = null

    /** True when [route] differs from what was last drawn, and records it as drawn. */
    fun needsRedraw(route: List<LatLngPoint>): Boolean {
        val newest = route.lastOrNull()
        if (hasDrawn && route.size == drawnCount && newest == drawnNewest) return false

        hasDrawn = true
        drawnCount = route.size
        drawnNewest = newest
        return true
    }

    /**
     * Forgets what is on screen, so the next route is drawn even if it is unchanged.
     *
     * Called when a new [com.google.android.gms.maps.GoogleMap] is bound: it holds none of the
     * overlay built on the previous one, so the gate's idea of what is drawn no longer refers to
     * anything.
     */
    fun invalidate() {
        hasDrawn = false
        drawnCount = 0
        drawnNewest = null
    }
}
