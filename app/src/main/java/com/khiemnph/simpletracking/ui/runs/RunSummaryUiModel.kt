package com.khiemnph.simpletracking.ui.runs

import com.khiemnph.domain.model.LatLngPoint

/**
 * Pre-formatted, display-ready representation of a [com.khiemnph.domain.model.SessionSummary] row.
 * All strings are already formatted by [RunsViewModel], and [routePoints] is already decoded, so
 * [RunsScreen] only ever draws what it is given.
 */
data class RunSummaryUiModel(
    val id: String,
    val recordedAtLabel: String,
    /** The number alone, e.g. "0.21". The unit is applied by the composable, from resources. */
    val distanceKm: String,
    val durationLabel: String,
    val averageSpeedKmh: String,
    /** Empty when the session has no recorded shape, which the row renders as its empty state. */
    val routePoints: List<LatLngPoint>,
)
