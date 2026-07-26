package com.khiemnph.simpletracking.ui.history

import com.khiemnph.domain.model.SessionSummary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import com.khiemnph.simpletracking.testing.DefaultLocaleRule
import org.junit.Rule
import org.junit.Test

class HistoryGroupingTest {

    @get:Rule
    val localeRule = DefaultLocaleRule()

    private val zone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")
    private val today = LocalDate.of(2026, 7, 26)

    private fun summaryOn(
        date: LocalDate,
        hour: Int = 9,
        distanceMeters: Double = 5_000.0,
        durationMillis: Long = 1_800_000L,
        id: String = "$date-$hour",
    ) = SessionSummary(
        id = id,
        distanceMeters = distanceMeters,
        durationMillis = durationMillis,
        averageSpeedMps = 2.8f,
        routePolyline = null,
        recordedAt = date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli(),
    )

    @Test
    fun givenRunsToday_whenGrouped_thenTheHeadingSaysToday() {
        val groups = HistoryGrouping.groupsFor(listOf(summaryOn(today)), today, zone)

        assertEquals("Today", groups.single().label)
    }

    @Test
    fun givenRunsYesterday_whenGrouped_thenTheHeadingSaysYesterday() {
        val groups = HistoryGrouping.groupsFor(listOf(summaryOn(today.minusDays(1))), today, zone)

        assertEquals("Yesterday", groups.single().label)
    }

    @Test
    fun givenAnOlderRun_whenGrouped_thenTheHeadingIsItsDate() {
        val groups = HistoryGrouping.groupsFor(listOf(summaryOn(LocalDate.of(2026, 7, 4))), today, zone)

        assertEquals("Sat, 4 Jul", groups.single().label)
    }

    @Test
    fun givenTwoRunsOnTheSameDay_whenGrouped_thenTheyShareOneHeading() {
        val summaries = listOf(summaryOn(today, hour = 6), summaryOn(today, hour = 18))

        val groups = HistoryGrouping.groupsFor(summaries, today, zone)

        assertEquals(1, groups.size)
        assertEquals(2, groups.single().sessions.size)
    }

    /** Grouping must not reorder: the repository already returns newest first. */
    @Test
    fun givenRunsAcrossDays_whenGrouped_thenGroupOrderFollowsTheInputOrder() {
        val summaries = listOf(summaryOn(today), summaryOn(today.minusDays(1)), summaryOn(today.minusDays(5)))

        val groups = HistoryGrouping.groupsFor(summaries, today, zone)

        assertEquals(listOf("Today", "Yesterday", "Tue, 21 Jul"), groups.map { it.label })
    }

    /**
     * A run just before midnight and one just after belong to different days. Getting this wrong is
     * invisible in most testing because it only shows up near the boundary.
     */
    @Test
    fun givenRunsEitherSideOfMidnight_whenGrouped_thenTheyAreSeparateDays() {
        val lateLastNight = summaryOn(today.minusDays(1), hour = 23, id = "late")
        val earlyToday = summaryOn(today, hour = 0, id = "early")

        val groups = HistoryGrouping.groupsFor(listOf(earlyToday, lateLastNight), today, zone)

        assertEquals(listOf("Today", "Yesterday"), groups.map { it.label })
    }

    @Test
    fun givenRunsInTheLastSevenDays_whenSummarised_thenTotalsCoverOnlyThatWindow() {
        val summaries = listOf(
            summaryOn(today, distanceMeters = 5_000.0, durationMillis = 1_800_000L),
            summaryOn(today.minusDays(3), distanceMeters = 3_000.0, durationMillis = 1_200_000L),
            // Outside the window, and must not be counted.
            summaryOn(today.minusDays(9), distanceMeters = 99_000.0, durationMillis = 9_000_000L),
        )

        val week = HistoryGrouping.weekFor(summaries, today, zone)

        assertEquals("8.00 km", week.distanceLabel)
        assertEquals("2 runs", week.runCountLabel)
        assertEquals("50:00", week.durationLabel)
    }

    @Test
    fun givenASingleRunThisWeek_whenSummarised_thenTheCountIsSingular() {
        val week = HistoryGrouping.weekFor(listOf(summaryOn(today)), today, zone)

        assertEquals("1 run", week.runCountLabel)
    }

    @Test
    fun givenNoRunsThisWeek_whenSummarised_thenEveryBarIsZero() {
        val week = HistoryGrouping.weekFor(listOf(summaryOn(today.minusDays(30))), today, zone)

        assertEquals(7, week.dailyDistanceFractions.size)
        assertEquals(0f, week.dailyDistanceFractions.max(), 0.0001f)
    }

    @Test
    fun givenRunsOnDifferentDays_whenSummarised_thenBarsAreOldestFirstAndScaledToTheBestDay() {
        val summaries = listOf(
            summaryOn(today, distanceMeters = 10_000.0),
            summaryOn(today.minusDays(6), distanceMeters = 5_000.0),
        )

        val week = HistoryGrouping.weekFor(summaries, today, zone)

        assertEquals(7, week.dailyDistanceFractions.size)
        assertEquals(0.5f, week.dailyDistanceFractions.first(), 0.0001f)
        assertEquals(1f, week.dailyDistanceFractions.last(), 0.0001f)
    }

    @Test
    fun givenTwoRunsOnOneDay_whenSummarised_thenThatDaysBarCombinesThem() {
        val summaries = listOf(
            summaryOn(today, hour = 6, distanceMeters = 4_000.0, id = "a"),
            summaryOn(today, hour = 18, distanceMeters = 4_000.0, id = "b"),
            summaryOn(today.minusDays(2), distanceMeters = 8_000.0, id = "c"),
        )

        val week = HistoryGrouping.weekFor(summaries, today, zone)

        // Today totals 8 km, matching the other day, so both bars are full height.
        assertEquals(1f, week.dailyDistanceFractions.last(), 0.0001f)
    }
}
