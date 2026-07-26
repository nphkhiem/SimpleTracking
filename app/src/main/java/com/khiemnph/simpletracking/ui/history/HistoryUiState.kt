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

    data class Sessions(val sessions: List<HistorySummaryUiModel>) : HistoryUiState
}
