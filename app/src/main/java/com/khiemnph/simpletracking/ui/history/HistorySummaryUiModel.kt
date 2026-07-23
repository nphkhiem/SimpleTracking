package com.khiemnph.simpletracking.ui.history

/**
 * Pre-formatted, display-ready representation of a [com.khiemnph.domain.model.SessionSummary]
 * row. All strings are already formatted by [HistoryViewModel] so [HistoryAdapter] only ever
 * binds them, never formats anything itself.
 */
data class HistorySummaryUiModel(
    val id: String,
    val distanceLabel: String,
    val durationLabel: String,
    val averageSpeedLabel: String,
    val thumbnailPath: String?,
)
