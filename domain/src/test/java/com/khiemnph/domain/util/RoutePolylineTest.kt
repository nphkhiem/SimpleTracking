package com.khiemnph.domain.util

import com.khiemnph.domain.model.LatLngPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutePolylineTest {

    private fun route(count: Int) = (0 until count).map {
        LatLngPoint(latitude = 21.0285 + it * 0.0001, longitude = 105.8542 + it * 0.0001)
    }

    @Test
    fun givenAnEncodedRoute_whenDecoded_thenTheShapeIsRecoveredWithinThumbnailPrecision() {
        val original = route(5)

        val decoded = RoutePolyline.decode(RoutePolyline.encode(original)!!)

        assertEquals(original.size, decoded.size)
        original.zip(decoded).forEach { (a, b) ->
            assertEquals(a.latitude, b.latitude, RoutePolyline.PRECISION_DEGREES)
            assertEquals(a.longitude, b.longitude, RoutePolyline.PRECISION_DEGREES)
        }
    }

    @Test
    fun givenARouteLongerThanTheThumbnailNeeds_whenEncoded_thenItIsDownsampled() {
        val encoded = RoutePolyline.encode(route(2_000))

        assertEquals(RoutePolyline.MAX_POINTS, RoutePolyline.decode(encoded!!).size)
    }

    @Test
    fun givenADownsampledRoute_whenDecoded_thenItStillStartsAndEndsWhereTheRunDid() {
        val original = route(2_000)

        val decoded = RoutePolyline.decode(RoutePolyline.encode(original)!!)

        assertEquals(original.first().latitude, decoded.first().latitude, RoutePolyline.PRECISION_DEGREES)
        assertEquals(original.last().latitude, decoded.last().latitude, RoutePolyline.PRECISION_DEGREES)
    }

    @Test
    fun givenARouteShorterThanTheLimit_whenEncoded_thenEveryPointIsKept() {
        assertEquals(7, RoutePolyline.decode(RoutePolyline.encode(route(7))!!).size)
    }

    @Test
    fun givenNoPoints_whenEncoded_thenTheResultIsNull() {
        assertEquals(null, RoutePolyline.encode(emptyList()))
    }

    @Test
    fun givenASinglePoint_whenEncoded_thenTheResultIsNullBecauseOnePointIsNotARoute() {
        assertEquals(null, RoutePolyline.encode(route(1)))
    }

    @Test
    fun givenMalformedText_whenDecoded_thenTheResultIsEmptyRatherThanAThrow() {
        assertTrue(RoutePolyline.decode("not a route").isEmpty())
        assertTrue(RoutePolyline.decode("").isEmpty())
    }

    /**
     * The history query reads this column for every stopped session on every emission, so the
     * per-row cost is a property worth pinning rather than discovering later with a long history.
     */
    @Test
    fun givenALongRoute_whenEncoded_thenTheStoredTextStaysAroundAKilobyte() {
        val encoded = RoutePolyline.encode(route(2_000))!!

        assertTrue("Encoded route was ${encoded.length} chars", encoded.length < 1_200)
    }
}
