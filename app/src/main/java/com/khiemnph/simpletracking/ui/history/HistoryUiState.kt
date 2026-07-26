package com.khiemnph.simpletracking.ui.history

/**
 * What the session list has to show right now.
 *
 * [Loading] exists so [Empty] means something. With a bare list the screen could not tell "no runs
 * recorded yet" from "the database has not answered", and a first-run user would see the empty
 * state flash before their history appeared.
 */
sealed interface HistoryUiState {

    data object Loading : HistoryUiState

    data object Empty : HistoryUiState

    data class Sessions(
        val week: WeekSummaryUiModel,
        val groups: List<SessionGroupUiModel>,
    ) : HistoryUiState
}

/** Runs recorded on the same day, under one heading. */
data class SessionGroupUiModel(
    val label: String,
    val sessions: List<HistorySummaryUiModel>,
)

/**
 * The last seven days at a glance.
 *
 * [dailyDistanceFractions] is one value per day, oldest first, each between 0 and 1 relative to the
 * week's best day. Already normalised here so the bar strip only has to draw heights, and a day
 * with no run is simply 0 rather than a missing entry.
 */
data class WeekSummaryUiModel(
    val distanceLabel: String,
    val runCountLabel: String,
    val durationLabel: String,
    val dailyDistanceFractions: List<Float>,
)
