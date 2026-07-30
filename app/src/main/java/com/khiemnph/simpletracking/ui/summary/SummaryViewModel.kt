package com.khiemnph.simpletracking.ui.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khiemnph.domain.interactor.DeleteSessionUseCase
import com.khiemnph.domain.interactor.ObserveSessionSummaryUseCase
import com.khiemnph.domain.util.RoutePolyline
import com.khiemnph.simpletracking.ui.format.formatDistanceKm
import com.khiemnph.simpletracking.ui.format.formatDuration
import com.khiemnph.simpletracking.ui.record.formatPaceMinPerKm
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Below this, a run is almost certainly a mistake rather than an activity: Record tapped and
 * stopped again, or a session left running while stationary. Short enough that a genuine sprint
 * still counts.
 */
private const val TOO_SHORT_METERS = 100.0

/**
 * How long to wait for the Service to finish writing the run before concluding it is genuinely
 * gone. Comfortably longer than the write takes, short enough that a truly missing session does not
 * leave the screen spinning.
 */
private const val WAIT_FOR_FINAL_STATS_MILLIS = 5_000L

/**
 * Backs the post-run screen.
 *
 * The run is already durably saved before this screen opens: `StopSessionUseCase` writes the final
 * stats as part of stopping. So there is no save to perform here and no save that can fail. The
 * primary action just leaves, and the only destructive path is discarding.
 */
@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val observeSessionSummaryUseCase: ObserveSessionSummaryUseCase,
    private val deleteSessionUseCase: DeleteSessionUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val sessionId: String = requireNotNull(savedStateHandle["sessionId"]) {
        "SummaryFragment requires a sessionId argument"
    }

    private val _uiState = MutableStateFlow<SummaryUiState>(SummaryUiState.Loading)
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Waits rather than reading once. This screen is reached the instant Stop is
            // dispatched, and the Service writes the final stats asynchronously after that, so a
            // single read lands before the row is finished and reports a run that does exist as
            // missing. Verified on device: it did exactly that.
            val summary = withTimeoutOrNull(WAIT_FOR_FINAL_STATS_MILLIS) {
                observeSessionSummaryUseCase(sessionId).filterNotNull().first()
            }
            _uiState.value = if (summary == null) {
                SummaryUiState.NotFound
            } else {
                SummaryUiState.Ready(
                    distanceKm = formatDistanceKm(summary.distanceMeters),
                    durationLabel = formatDuration(summary.durationMillis),
                    // Derived from distance over time rather than the stored average speed, so the
                    // three numbers on screen always agree with each other.
                    paceLabel = formatPaceMinPerKm(averageSpeedMps(summary.distanceMeters, summary.durationMillis)),
                    routePoints = summary.routePolyline?.let(RoutePolyline::decode).orEmpty(),
                    isTooShortToKeep = summary.distanceMeters < TOO_SHORT_METERS,
                )
            }
        }
    }

    /** Removes the run. The caller navigates away; this does not depend on the screen surviving. */
    fun onDiscard(onDiscarded: () -> Unit) {
        viewModelScope.launch {
            deleteSessionUseCase(sessionId)
            onDiscarded()
        }
    }

    private fun averageSpeedMps(distanceMeters: Double, durationMillis: Long): Float =
        if (durationMillis <= 0L) 0f else (distanceMeters / (durationMillis / 1_000.0)).toFloat()
}
