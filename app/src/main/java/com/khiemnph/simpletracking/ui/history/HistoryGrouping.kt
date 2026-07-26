package com.khiemnph.simpletracking.ui.history

import com.khiemnph.domain.model.SessionSummary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val METERS_PER_KILOMETER = 1_000.0
private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L
private const val DAYS_IN_WEEK = 7

private val GROUP_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.US)

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
            distanceLabel = String.format(Locale.US, "%.2f km", thisWeek.sumOf { it.distanceMeters } / METERS_PER_KILOMETER),
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
        else -> GROUP_DATE_FORMATTER.format(date)
    }

    private fun Long.toLocalDate(zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

    private fun formatDuration(durationMillis: Long): String {
        val totalSeconds = (durationMillis / MILLIS_PER_SECOND).coerceAtLeast(0L)
        val hours = totalSeconds / SECONDS_PER_HOUR
        val minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
        val seconds = totalSeconds % SECONDS_PER_MINUTE
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }
}
