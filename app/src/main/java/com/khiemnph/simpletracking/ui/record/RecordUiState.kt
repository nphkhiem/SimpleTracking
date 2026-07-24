package com.khiemnph.simpletracking.ui.record

import com.khiemnph.domain.model.LatLngPoint
import com.khiemnph.domain.model.SessionStatus

/**
 * Display-ready snapshot of the active session for [RecordFragment]. [currentSpeedMps] is the
 * presentation-only smoothed value [RecordViewModel] maintains; every other field is a direct,
 * unsmoothed pass-through of [com.khiemnph.domain.model.ActiveSessionState].
 */
data class RecordUiState(
    val status: SessionStatus = SessionStatus.RUNNING,
    val distanceMeters: Double = 0.0,
    val elapsedDurationMillis: Long = 0L,
    val currentSpeedMps: Float = 0f,
    val averageSpeedMps: Float = 0f,
    val route: List<LatLngPoint> = emptyList(),
)
