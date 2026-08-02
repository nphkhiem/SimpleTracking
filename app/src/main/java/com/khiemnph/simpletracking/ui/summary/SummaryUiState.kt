package com.khiemnph.simpletracking.ui.summary

import com.khiemnph.domain.model.LatLngPoint

/** What the post-run screen has to say about the run that just finished. */
sealed interface SummaryUiState {

    data object Loading : SummaryUiState

    /**
     * The session could not be read. Reachable if it was deleted from another surface while this
     * screen was opening, or if the id no longer resolves. The screen offers a way back rather
     * than showing an empty shell.
     */
    data object NotFound : SummaryUiState

    data class Ready(
        val distanceKm: String,
        val durationLabel: String,
        val paceLabel: String,
        val routePoints: List<LatLngPoint>,
        /**
         * True when the run is too short to be worth keeping. The screen offers to discard it,
         * which is what stops a tap-Record-then-tap-Stop mistake becoming a permanent 0.00 km row.
         */
    ) : SummaryUiState
}
