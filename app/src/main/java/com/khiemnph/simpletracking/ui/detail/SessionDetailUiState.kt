package com.khiemnph.simpletracking.ui.detail

import com.khiemnph.domain.model.LatLngPoint

/** One row of the splits list, already formatted and already sized for its bar. */
data class SplitUiModel(
    val label: String,
    val paceLabel: String,
    /** 0 to 1, relative to the slowest split, so the bars compare within this run only. */
    val barFraction: Float,
    val isFastest: Boolean,
)

sealed interface SessionDetailUiState {

    data object Loading : SessionDetailUiState

    /** The session is gone: an unknown id, or deleted while this screen was open. */
    data object NotFound : SessionDetailUiState

    data class Ready(
        /** The user's name for the run, or its date when never named. */
        val titleLabel: String,
        /** True when [titleLabel] is a name the user chose, so Rename can pre-fill it. */
        val hasCustomTitle: Boolean,
        val distanceKm: String,
        val durationLabel: String,
        val averagePaceLabel: String,
        val bestPaceLabel: String,
        val routePoints: List<LatLngPoint>,
        val splits: List<SplitUiModel>,
    ) : SessionDetailUiState
}
