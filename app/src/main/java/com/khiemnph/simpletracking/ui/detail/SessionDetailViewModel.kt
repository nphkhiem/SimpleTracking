package com.khiemnph.simpletracking.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khiemnph.domain.interactor.DeleteSessionUseCase
import com.khiemnph.domain.interactor.GetSessionSplitsUseCase
import com.khiemnph.domain.interactor.ObserveSessionSummaryUseCase
import com.khiemnph.domain.interactor.RenameSessionUseCase
import com.khiemnph.domain.model.SessionSummary
import com.khiemnph.domain.util.RoutePolyline
import com.khiemnph.domain.util.Split
import com.khiemnph.simpletracking.ui.format.formatDistanceKm
import com.khiemnph.simpletracking.ui.format.formatDuration
import com.khiemnph.simpletracking.ui.record.formatPaceMinPerKm
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The smallest bar still readable as a bar rather than a hairline. */
private const val MINIMUM_BAR_FRACTION = 0.08f

/** Resolved per call so a device language change is picked up rather than frozen at class load. */
private fun titleFormatter(): DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a", Locale.getDefault())

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val observeSessionSummaryUseCase: ObserveSessionSummaryUseCase,
    private val getSessionSplitsUseCase: GetSessionSplitsUseCase,
    private val renameSessionUseCase: RenameSessionUseCase,
    private val deleteSessionUseCase: DeleteSessionUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val sessionId: String = requireNotNull(savedStateHandle["sessionId"]) {
        "SessionDetailFragment requires a sessionId argument"
    }

    private val _uiState = MutableStateFlow<SessionDetailUiState>(SessionDetailUiState.Loading)
    val uiState: StateFlow<SessionDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Observed rather than read once, so a session deleted from elsewhere while this screen
            // is open collapses to NotFound instead of showing stats for a run that is gone.
            observeSessionSummaryUseCase(sessionId).collect { summary ->
                _uiState.value = if (summary == null) {
                    SessionDetailUiState.NotFound
                } else {
                    ready(summary, getSessionSplitsUseCase(sessionId))
                }
            }
        }
    }

    fun onRename(title: String) {
        viewModelScope.launch { renameSessionUseCase(sessionId, title) }
    }

    fun onDelete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            deleteSessionUseCase(sessionId)
            onDeleted()
        }
    }

    private fun ready(summary: SessionSummary, splits: List<Split>): SessionDetailUiState.Ready {
        // Complete splits only: a 200 m closing split extrapolates to a pace that was never run,
        // and letting it win "best" would make the number a lie.
        val bestPace = splits.filterNot { it.isPartial }.minByOrNull { it.paceSecondsPerKm }
        val slowestPace = splits.maxOfOrNull { it.paceSecondsPerKm } ?: 0.0

        return SessionDetailUiState.Ready(
            titleLabel = summary.title ?: Instant.ofEpochMilli(summary.recordedAt)
                .atZone(ZoneId.systemDefault())
                .format(titleFormatter()),
            hasCustomTitle = summary.title != null,
            distanceKm = formatDistanceKm(summary.distanceMeters),
            durationLabel = formatDuration(summary.durationMillis),
            averagePaceLabel = formatPaceMinPerKm(averageSpeedMps(summary.distanceMeters, summary.durationMillis)),
            bestPaceLabel = bestPace?.let { formatPaceMinPerKm(speedFromPace(it.paceSecondsPerKm)) }
                ?: formatPaceMinPerKm(0f),
            routePoints = summary.routePolyline?.let(RoutePolyline::decode).orEmpty(),
            splits = splits.map { split ->
                SplitUiModel(
                    label = if (split.isPartial) {
                        formatDistanceKm(split.distanceMeters)
                    } else {
                        split.index.toString()
                    },
                    paceLabel = formatPaceMinPerKm(speedFromPace(split.paceSecondsPerKm)),
                    // Longer bar means slower, so the chart reads as time spent per kilometre.
                    barFraction = if (slowestPace <= 0.0) {
                        MINIMUM_BAR_FRACTION
                    } else {
                        (split.paceSecondsPerKm / slowestPace).toFloat().coerceAtLeast(MINIMUM_BAR_FRACTION)
                    },
                    isFastest = bestPace != null && split.index == bestPace.index,
                )
            },
        )
    }

    private fun averageSpeedMps(distanceMeters: Double, durationMillis: Long): Float =
        if (durationMillis <= 0L) 0f else (distanceMeters / (durationMillis / 1_000.0)).toFloat()

    private fun speedFromPace(secondsPerKm: Double): Float =
        if (secondsPerKm <= 0.0) 0f else (1_000.0 / secondsPerKm).toFloat()
}
