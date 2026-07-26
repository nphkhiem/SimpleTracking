package com.khiemnph.simpletracking.ui.route

import androidx.compose.ui.geometry.Size
import com.khiemnph.domain.model.LatLngPoint
import kotlin.math.abs
import kotlin.math.cos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val METERS_PER_DEGREE_LATITUDE = 111_320.0
private const val TOLERANCE_PX = 0.5f

/** A degree count that spans [meters] north-south. */
private fun degreesNorth(meters: Double) = meters / METERS_PER_DEGREE_LATITUDE

class RouteProjectionTest {

    private val square = Size(1000f, 1000f)

    @Test
    fun `returns null for a single point`() {
        val projected = RouteProjection.project(
            points = listOf(LatLngPoint(21.0, 105.0)),
            size = square,
            insetPx = 0f,
        )

        assertNull(projected)
    }

    @Test
    fun `returns null for an empty route`() {
        assertNull(RouteProjection.project(points = emptyList(), size = square, insetPx = 0f))
    }

    @Test
    fun `returns null when the inset leaves no room to draw`() {
        val projected = RouteProjection.project(
            points = listOf(LatLngPoint(0.0, 0.0), LatLngPoint(0.01, 0.01)),
            size = Size(20f, 20f),
            insetPx = 10f,
        )

        assertNull(projected)
    }

    @Test
    fun `a due-north route draws as a vertical line filling the height`() {
        val projected = RouteProjection.project(
            points = listOf(LatLngPoint(0.0, 0.0), LatLngPoint(degreesNorth(1000.0), 0.0)),
            size = square,
            insetPx = 0f,
        )!!

        val (first, last) = projected.offsets.first() to projected.offsets.last()
        assertEquals(first.x, last.x, TOLERANCE_PX)
        assertEquals(1000f, abs(last.y - first.y), TOLERANCE_PX)
    }

    @Test
    fun `latitude increases upward so the northern point sits above the southern one`() {
        val projected = RouteProjection.project(
            points = listOf(LatLngPoint(0.0, 0.0), LatLngPoint(degreesNorth(1000.0), 0.0)),
            size = square,
            insetPx = 0f,
        )!!

        val south = projected.offsets.first()
        val north = projected.offsets.last()
        assertTrue("north should have the smaller y", north.y < south.y)
    }

    @Test
    fun `meters per pixel reflects the real ground distance spanned`() {
        // 1000 m of latitude fitted to 1000 px of height is exactly one metre per pixel.
        val projected = RouteProjection.project(
            points = listOf(LatLngPoint(0.0, 0.0), LatLngPoint(degreesNorth(1000.0), 0.0)),
            size = square,
            insetPx = 0f,
        )!!

        assertEquals(1.0f, projected.metersPerPixel!!, 0.001f)
    }

    @Test
    fun `longitude is scaled by the cosine of latitude rather than treated as equal to latitude`() {
        // One degree each way at 60 degrees north: the longitude side covers roughly half the
        // ground distance of the latitude side, so it must be drawn roughly half as wide as tall.
        val projected = RouteProjection.project(
            points = listOf(LatLngPoint(60.0, 0.0), LatLngPoint(61.0, 1.0)),
            size = square,
            insetPx = 0f,
        )!!

        val width = abs(projected.offsets.last().x - projected.offsets.first().x)
        val height = abs(projected.offsets.last().y - projected.offsets.first().y)
        val expectedRatio = cos(Math.toRadians(60.5)).toFloat()

        assertEquals(expectedRatio, width / height, 0.01f)
    }

    @Test
    fun `a route that is square on the ground stays square on a non-square canvas`() {
        val side = 500.0
        val north = degreesNorth(side)
        val east = side / (METERS_PER_DEGREE_LATITUDE * cos(0.0))
        val projected = RouteProjection.project(
            points = listOf(
                LatLngPoint(0.0, 0.0),
                LatLngPoint(north, 0.0),
                LatLngPoint(north, east),
                LatLngPoint(0.0, east),
            ),
            size = Size(1200f, 400f),
            insetPx = 0f,
        )!!

        val width = projected.offsets.maxOf { it.x } - projected.offsets.minOf { it.x }
        val height = projected.offsets.maxOf { it.y } - projected.offsets.minOf { it.y }
        assertEquals(1f, width / height, 0.01f)
    }

    @Test
    fun `the drawn route is centred within the box`() {
        val projected = RouteProjection.project(
            points = listOf(LatLngPoint(0.0, 0.0), LatLngPoint(degreesNorth(1000.0), 0.0)),
            size = Size(1200f, 400f),
            insetPx = 0f,
        )!!

        // The route has no width, so it must sit on the horizontal centre line.
        assertEquals(600f, projected.offsets.first().x, TOLERANCE_PX)
    }

    @Test
    fun `the inset keeps the route clear of the edges`() {
        val projected = RouteProjection.project(
            points = listOf(LatLngPoint(0.0, 0.0), LatLngPoint(degreesNorth(1000.0), 0.0)),
            size = square,
            insetPx = 50f,
        )!!

        assertEquals(50f, projected.offsets.minOf { it.y }, TOLERANCE_PX)
        assertEquals(950f, projected.offsets.maxOf { it.y }, TOLERANCE_PX)
    }

    @Test
    fun `coincident points collapse to the centre without dividing by zero`() {
        val point = LatLngPoint(21.0, 105.0)
        val projected = RouteProjection.project(
            points = listOf(point, point, point),
            size = square,
            insetPx = 0f,
        )!!

        assertTrue(projected.offsets.all { it.x == 500f && it.y == 500f })
        assertNull("scale is meaningless with no extent", projected.metersPerPixel)
    }

    @Test
    fun `every point is projected`() {
        val points = List(7) { LatLngPoint(21.0 + it * 0.001, 105.0 + it * 0.001) }

        val projected = RouteProjection.project(points, square, insetPx = 0f)

        assertNotNull(projected)
        assertEquals(points.size, projected!!.offsets.size)
    }
}
