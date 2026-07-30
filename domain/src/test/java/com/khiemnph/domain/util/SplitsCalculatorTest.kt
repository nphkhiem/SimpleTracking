package com.khiemnph.domain.util

import com.khiemnph.domain.model.LocationPoint
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Must match [DistanceCalculator]'s own earth model, or a fixture that says "2000 m" measures
 * something else and split boundaries land in the wrong place. Derived rather than written out, so
 * the two cannot drift apart.
 */
private val METERS_PER_DEGREE_LATITUDE = 6_371_000.0 * Math.PI / 180.0

class SplitsCalculatorTest {

    /** A point [northMeters] north of the start, [atMillis] into the run. */
    private fun point(northMeters: Double, atMillis: Long) = LocationPoint(
        sessionId = "s",
        latitude = northMeters / METERS_PER_DEGREE_LATITUDE,
        longitude = 0.0,
        timestamp = atMillis,
        horizontalAccuracyMeters = 5f,
        speedMetersPerSec = 3f,
    )

    /** A straight run of [totalMeters] at a steady [metersPerSecond], sampled every second. */
    private fun steadyRun(totalMeters: Double, metersPerSecond: Double): List<LocationPoint> {
        val seconds = (totalMeters / metersPerSecond).toInt()
        return (0..seconds).map { s -> point(s * metersPerSecond, s * 1_000L) }
    }

    @Test
    fun `a run shorter than one kilometre is a single partial split`() {
        val splits = SplitsCalculator.splitsFor(steadyRun(400.0, 4.0))

        assertEquals(1, splits.size)
        assertTrue("an unfinished kilometre must say so", splits.single().isPartial)
        assertEquals(400.0, splits.single().distanceMeters, 5.0)
    }

    @Test
    fun `a run of exactly two kilometres is two complete splits`() {
        val splits = SplitsCalculator.splitsFor(steadyRun(2_000.0, 4.0))

        assertEquals(2, splits.size)
        // `none`, not `!all`: the weaker form passed while the second kilometre was silently
        // partial, because the fixture's earth model did not match the calculator's.
        assertTrue("both kilometres must be complete", splits.none { it.isPartial })
        splits.forEach { assertEquals(1_000.0, it.distanceMeters, 5.0) }
    }

    @Test
    fun `a run of two and a half kilometres ends in a partial split`() {
        val splits = SplitsCalculator.splitsFor(steadyRun(2_500.0, 4.0))

        assertEquals(3, splits.size)
        assertFalse(splits[0].isPartial)
        assertFalse(splits[1].isPartial)
        assertTrue(splits[2].isPartial)
        assertEquals(500.0, splits[2].distanceMeters, 10.0)
    }

    @Test
    fun `splits are numbered from one`() {
        val splits = SplitsCalculator.splitsFor(steadyRun(2_000.0, 4.0))

        assertEquals(listOf(1, 2), splits.map { it.index })
    }

    @Test
    fun `a steady run produces near-identical split durations`() {
        // 4 m/s is 250 s per kilometre.
        val splits = SplitsCalculator.splitsFor(steadyRun(2_000.0, 4.0))

        splits.forEach { assertTrue("expected ~250 s, got ${it.durationMillis}", abs(it.durationMillis - 250_000L) < 3_000L) }
    }

    @Test
    fun `a second kilometre run slower takes proportionally longer`() {
        val fast = steadyRun(1_000.0, 5.0)
        val slowStart = fast.last().timestamp
        // A full second kilometre at half the pace: 2.5 m/s for 400 s.
        val slow = (1..400).map { s -> point(1_000.0 + s * 2.5, slowStart + s * 1_000L) }

        val splits = SplitsCalculator.splitsFor(fast + slow)

        assertEquals(2, splits.size)
        assertTrue("the slower kilometre must take longer", splits[1].durationMillis > splits[0].durationMillis)
        assertEquals(200_000.0, splits[0].durationMillis.toDouble(), 3_000.0)
        assertEquals(400_000.0, splits[1].durationMillis.toDouble(), 3_000.0)
    }

    @Test
    fun `split distances sum to the same total the headline distance reports`() {
        // Splits that do not add up to the number shown at the top of the screen are worse than no
        // splits at all, so both use the same drift rule.
        val points = steadyRun(2_500.0, 4.0)

        val fromSplits = SplitsCalculator.splitsFor(points).sumOf { it.distanceMeters }

        assertEquals(DistanceCalculator.travelledDistanceMeters(points), fromSplits, 0.001)
    }

    @Test
    fun `stationary drift is excluded from splits, exactly as it is from the total`() {
        val moving = steadyRun(1_000.0, 4.0)
        val lastAt = moving.last().timestamp
        // Standing still: sub-threshold hops arriving faster than the time threshold.
        val drifting = (1..20).map { s -> point(1_000.0 + s * 0.5, lastAt + s * 1_000L) }

        val splits = SplitsCalculator.splitsFor(moving + drifting)

        assertEquals(1, splits.size)
        assertFalse("drift must not create a phantom second kilometre", splits.any { it.index == 2 })
    }

    @Test
    fun `fewer than two points has no splits rather than a zero-length one`() {
        assertEquals(emptyList<Split>(), SplitsCalculator.splitsFor(emptyList()))
        assertEquals(emptyList<Split>(), SplitsCalculator.splitsFor(listOf(point(0.0, 0L))))
    }

    @Test
    fun `a run that never moves has no splits`() {
        val stationary = (0..30).map { s -> point(0.0, s * 1_000L) }

        assertEquals(emptyList<Split>(), SplitsCalculator.splitsFor(stationary))
    }

    @Test
    fun `the fastest split is the one with the lowest pace`() {
        val first = steadyRun(1_000.0, 4.0)
        val secondStart = first.last().timestamp
        val second = (1..125).map { s -> point(1_000.0 + s * 8.0, secondStart + s * 1_000L) }

        val splits = SplitsCalculator.splitsFor(first + second)

        assertEquals(2, splits.minByOrNull { it.paceSecondsPerKm }?.index)
    }

    @Test
    fun `a run ending exactly on a kilometre mark completes that kilometre`() {
        // Summing hundreds of haversine hops lands a fraction under the boundary, which used to
        // report a full kilometre as a partial split one ten-millionth of a metre short.
        val splits = SplitsCalculator.splitsFor(steadyRun(3_000.0, 4.0))

        assertEquals(3, splits.size)
        assertTrue("a run ending on the mark has no partial split", splits.none { it.isPartial })
    }
}
