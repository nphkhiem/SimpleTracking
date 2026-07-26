package com.khiemnph.domain.util

import com.khiemnph.domain.model.LatLngPoint
import kotlin.math.roundToLong

/**
 * Compact text form of a route's shape, small enough to live on the session row and be read with
 * the rest of the summary in one query.
 *
 * This exists so a finished session can render its route without a live map. Drawing it from
 * geometry rather than snapshotting a `GoogleMap` means the result is deterministic, available
 * offline, sharp at any size, and able to follow the theme, none of which a captured PNG of map
 * tiles can do.
 *
 * The encoding is deliberately plain rather than Google's polyline algorithm: it is a
 * semicolon-separated list of fixed-point `lat,lng` pairs. That is a little larger on the wire, but
 * it is trivially readable in a database browser, has no shared decoder state to get wrong, and the
 * size is bounded by [MAX_POINTS] regardless of how long the run was.
 */
object RoutePolyline {

    /**
     * Enough shape for a thumbnail and a full-width detail hero without storing every fix. A 2-hour
     * run at one fix per 2 s is roughly 3,600 points; drawing those into a 56dp square would be
     * thousands of sub-pixel segments nobody can see.
     *
     * This bounds the stored text at roughly 1 KB per session, which matters because the history
     * query reads every stopped row on every emission. Raising it trades that read cost for detail
     * no thumbnail can show.
     */
    const val MAX_POINTS = 64

    /** Five decimal places, about a metre. Below what a thumbnail can express, and well below GPS error. */
    const val PRECISION_DEGREES = 0.00001

    private const val SCALE = 100_000.0
    private const val POINT_SEPARATOR = ";"
    private const val PAIR_SEPARATOR = ","

    /**
     * @return null when there is no line to draw. One point is a position, not a route, and callers
     * should render their empty state rather than a dot.
     */
    fun encode(points: List<LatLngPoint>): String? {
        if (points.size < 2) return null
        return downsample(points).joinToString(POINT_SEPARATOR) { point ->
            "${fixed(point.latitude)}$PAIR_SEPARATOR${fixed(point.longitude)}"
        }
    }

    /** Lenient by design: a row that cannot be parsed renders as no route, never as a crash. */
    fun decode(encoded: String): List<LatLngPoint> =
        encoded.split(POINT_SEPARATOR).mapNotNull { pair ->
            val parts = pair.split(PAIR_SEPARATOR)
            if (parts.size != 2) return@mapNotNull null
            val latitude = parts[0].toLongOrNull() ?: return@mapNotNull null
            val longitude = parts[1].toLongOrNull() ?: return@mapNotNull null
            LatLngPoint(latitude / SCALE, longitude / SCALE)
        }

    /**
     * Evenly spaced sample that always keeps the first and last point, so the drawn line starts and
     * ends where the run actually did rather than being clipped to a multiple of the stride.
     */
    private fun downsample(points: List<LatLngPoint>): List<LatLngPoint> {
        if (points.size <= MAX_POINTS) return points
        val lastIndex = points.lastIndex
        return (0 until MAX_POINTS).map { step ->
            points[(step.toDouble() * lastIndex / (MAX_POINTS - 1)).roundToInt()]
        }
    }

    private fun fixed(degrees: Double): Long = (degrees * SCALE).roundToLong()

    private fun Double.roundToInt(): Int = this.roundToLong().toInt()
}
