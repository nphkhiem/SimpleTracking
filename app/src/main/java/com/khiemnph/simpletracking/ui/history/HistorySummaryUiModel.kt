package com.khiemnph.simpletracking.ui.history

import com.khiemnph.domain.model.LatLngPoint

/**
 * Pre-formatted, display-ready representation of a [com.khiemnph.domain.model.SessionSummary] row.
 * All strings are already formatted by [HistoryViewModel], and [routePoints] is already decoded, so
 * [HistoryScreen] only ever draws what it is given.
 */
data class HistorySummaryUiModel(
    val id: String,
    val recordedAtLabel: String,
    val distanceLabel: String,
    val durationLabel: String,
    val averageSpeedLabel: String,
    /** Empty when the session has no recorded shape, which the row renders as its empty state. */
    val routePoints: List<LatLngPoint>,
)
