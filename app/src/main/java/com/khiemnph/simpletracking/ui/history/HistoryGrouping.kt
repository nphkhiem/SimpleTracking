package com.khiemnph.simpletracking.ui.history

import com.khiemnph.domain.model.SessionSummary
import com.khiemnph.simpletracking.ui.format.formatDistanceKm
import com.khiemnph.simpletracking.ui.format.formatDuration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val DAYS_IN_WEEK = 7

/**
 * Resolved per call rather than held in a top-level `val`, so a device language change is picked
 * up instead of being frozen at the moment the class first loaded.
 */
private fun groupDateFormatter(): DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())

/**
 * Turns a flat, newest-first list of sessions into the day groups and week summary the list shows.
 *
 * Pure functions taking an explicit `today` and time zone rather than reading the clock, because
 * every interesting case here is a date boundary: a run at 23:50 and one at 00:10 belong to
 * different days, and that is only testable if the day can be fixed by the caller.
 */
internal object HistoryGrouping {

    /** Preserves the input order; the repository already returns newest first. */
    fun groupsFor(
        summaries: List<SessionSummary>,
        today: LocalDate,
        zone: ZoneId,
    ): List<SessionGroupUiModel> =
        summaries
            .groupBy { it.recordedAt.toLocalDate(zone) }
            .map { (date, sessions) ->
                SessionGroupUiModel(
                    label = labelFor(date, today),
                    sessions = sessions.map { it.toHistorySummaryUiModel(zone) },
                )
            }

    fun weekFor(
        summaries: List<SessionSummary>,
        today: LocalDate,
        zone: ZoneId,
    ): WeekSummaryUiModel {
        // Oldest first, so the strip reads left to right and today is the rightmost bar.
        val days = (DAYS_IN_WEEK - 1 downTo 0).map { daysAgo -> today.minusDays(daysAgo.toLong()) }
        val byDay = summaries.groupBy { it.recordedAt.toLocalDate(zone) }
        val thisWeek = days.flatMap { byDay[it].orEmpty() }

        val perDayMeters = days.map { day -> byDay[day].orEmpty().sumOf { it.distanceMeters } }
        val best = perDayMeters.maxOrNull() ?: 0.0

        return WeekSummaryUiModel(
            distanceLabel = "${formatDistanceKm(thisWeek.sumOf { it.distanceMeters })} km",
            runCountLabel = if (thisWeek.size == 1) "1 run" else "${thisWeek.size} runs",
            durationLabel = formatDuration(thisWeek.sumOf { it.durationMillis }),
            dailyDistanceFractions = perDayMeters.map { meters ->
                if (best <= 0.0) 0f else (meters / best).toFloat()
            },
        )
    }

    private fun labelFor(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> groupDateFormatter().format(date)
    }

    private fun Long.toLocalDate(zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

}
