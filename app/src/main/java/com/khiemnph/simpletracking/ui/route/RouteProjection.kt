package com.khiemnph.simpletracking.ui.route

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.khiemnph.domain.model.LatLngPoint
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min

/** Mean metres per degree of latitude. Constant enough at a single run's scale. */
private const val METERS_PER_DEGREE_LATITUDE = 111_320.0

/**
 * A route laid out in a box, plus the ground scale that layout worked out to.
 *
 * [metersPerPixel] is null when every point is the same location: there is no extent to measure,
 * so any scale would be invented. Callers use that to suppress a scale bar rather than print a
 * number that means nothing.
 */
data class ProjectedRoute(
    val offsets: List<Offset>,
    val metersPerPixel: Float?,
)

/**
 * Projects latitude/longitude onto a drawing surface, preserving the route's real shape.
 *
 * Longitude is scaled by the cosine of the route's mid-latitude before fitting. Treating a degree
 * of longitude as equal to a degree of latitude - which is what a naive min/max normalisation does
 * - stretches the route east-west by 1/cos(latitude): about 7% in Hanoi and 35% in Oslo, so a lap
 * of a running track would come out visibly oval. This is the same local approximation a map makes
 * at these zoom levels, which is what lets the drawn shape match the one the user would see on a
 * map of the same run.
 *
 * The fitted shape is centred and uses one scale for both axes, so an out-and-back and a wide loop
 * both read correctly instead of being stretched to the frame.
 */
object RouteProjection {

    fun project(points: List<LatLngPoint>, size: Size, insetPx: Float): ProjectedRoute? {
        if (points.size < 2) return null

        val usableWidth = size.width - insetPx * 2
        val usableHeight = size.height - insetPx * 2
        if (usableWidth <= 0f || usableHeight <= 0f) return null

        val minLat = points.minOf { it.latitude }
        val maxLat = points.maxOf { it.latitude }
        val minLng = points.minOf { it.longitude }
        val maxLng = points.maxOf { it.longitude }

        val metersPerDegreeLongitude =
            METERS_PER_DEGREE_LATITUDE * abs(cos(Math.toRadians((minLat + maxLat) / 2)))

        val spanXMeters = (maxLng - minLng) * metersPerDegreeLongitude
        val spanYMeters = (maxLat - minLat) * METERS_PER_DEGREE_LATITUDE

        // A route can legitimately have extent on one axis only (a straight north-south run), in
        // which case that axis alone decides the scale. No extent at all has no scale to decide.
        val pixelsPerMeter = when {
            spanXMeters <= 0.0 && spanYMeters <= 0.0 -> null
            spanXMeters <= 0.0 -> usableHeight / spanYMeters
            spanYMeters <= 0.0 -> usableWidth / spanXMeters
            else -> min(usableWidth / spanXMeters, usableHeight / spanYMeters)
        }

        if (pixelsPerMeter == null || pixelsPerMeter <= 0.0) {
            val centre = Offset(size.width / 2f, size.height / 2f)
            return ProjectedRoute(offsets = points.map { centre }, metersPerPixel = null)
        }

        val drawnWidth = (spanXMeters * pixelsPerMeter).toFloat()
        val drawnHeight = (spanYMeters * pixelsPerMeter).toFloat()
        val originX = insetPx + (usableWidth - drawnWidth) / 2f
        val originY = insetPx + (usableHeight - drawnHeight) / 2f

        val offsets = points.map { point ->
            Offset(
                x = originX + ((point.longitude - minLng) * metersPerDegreeLongitude * pixelsPerMeter).toFloat(),
                // Latitude increases northward, y increases downward, so this axis is flipped.
                y = originY + drawnHeight - ((point.latitude - minLat) * METERS_PER_DEGREE_LATITUDE * pixelsPerMeter).toFloat(),
            )
        }

        return ProjectedRoute(offsets = offsets, metersPerPixel = (1.0 / pixelsPerMeter).toFloat())
    }
}
