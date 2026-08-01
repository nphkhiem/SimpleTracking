package com.khiemnph.simpletracking.ui.runs

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khiemnph.domain.interactor.DeleteSessionUseCase
import com.khiemnph.simpletracking.ui.format.formatDistanceKm
import com.khiemnph.simpletracking.ui.format.formatDuration
import com.khiemnph.simpletracking.ui.format.formatPaceMinPerKm
import com.khiemnph.domain.interactor.ObserveSessionHistoryUseCase
import com.khiemnph.domain.model.SessionSummary
import com.khiemnph.domain.util.RoutePolyline
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Resolved per call rather than held in a top-level `val`, so a device language change is picked
 * up instead of being frozen at the moment the class first loaded.
 */
private fun recordedAtFormatter(): DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, h:mm a", Locale.getDefault())

/**
 * Exposes [ObserveSessionHistoryUseCase]'s session summaries as pre-formatted
 * [RunSummaryUiModel]s for [RunsFragment]. Ordering is pass-through: the
 * underlying repository query already orders by `stoppedTimestamp DESC`, so this list is never
 * re-sorted here.
 *
 * Duration is formatted `m:ss` when under an hour and `h:mm:ss` once a session runs an hour or
 * longer - GPS tracking sessions (runs, rides) plausibly exceed an hour, and this mirrors the
 * common stopwatch/timer convention (e.g. Android's own `DateUtils.formatElapsedTime`) of not
 * zero-padding the leading unit.
 *
 * `recordedAt` (epoch millis) is formatted as `"EEE, h:mm a"` (e.g. `"Tue, 7:12 AM"`), matching
 * the reviewed mockup's per-row date/time label, resolved against the device's default time zone.
 */
private const val TAG = "RunsViewModel"

/** Long enough to read the snackbar and reach for Undo, short enough not to feel unfinished. */
private const val UNDO_WINDOW_MILLIS = 5_000L

@HiltViewModel
class RunsViewModel @Inject constructor(
    observeSessionHistoryUseCase: ObserveSessionHistoryUseCase,
    private val deleteSessionUseCase: DeleteSessionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RunsUiState>(RunsUiState.Loading)
    val uiState: StateFlow<RunsUiState> = _uiState.asStateFlow()

    /**
     * Sessions the user has swiped away but whose deletion has not been committed yet, so Undo can
     * still take it back. Held here rather than deleting immediately and re-inserting on undo: a
     * session's GPS points cannot be recreated once they are gone, so the safe direction is to
     * hide first and delete last.
     */
    private val pendingDeletions = MutableStateFlow<Set<String>>(emptySet())
    private val deletionJobs = mutableMapOf<String, Job>()

    private var latestSummaries: List<SessionSummary> = emptyList()

    /**
     * Swipe hides the row and starts the clock. If the ViewModel is cleared before it elapses the
     * delete simply never happens and the session reappears, which is the right way for this to
     * fail: nothing recorded is lost by a navigation.
     */
    fun onSessionSwipedAway(sessionId: String) {
        pendingDeletions.value += sessionId
        publish()
        deletionJobs[sessionId]?.cancel()
        deletionJobs[sessionId] = viewModelScope.launch {
            delay(UNDO_WINDOW_MILLIS)
            runCatching { deleteSessionUseCase(sessionId) }
                .onFailure { Log.e(TAG, "Could not delete session $sessionId", it) }
            pendingDeletions.value -= sessionId
            deletionJobs.remove(sessionId)
            publish()
        }
    }

    fun onUndoDelete(sessionId: String) {
        deletionJobs.remove(sessionId)?.cancel()
        pendingDeletions.value -= sessionId
        publish()
    }

    private fun publish() {
        val visible = latestSummaries.filterNot { it.id in pendingDeletions.value }
        _uiState.value = if (visible.isEmpty()) {
            RunsUiState.Empty
        } else {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            RunsUiState.Sessions(
                week = RunsGrouping.weekFor(visible, today, zone),
                groups = RunsGrouping.groupsFor(visible, today, zone),
            )
        }
    }

    init {
        viewModelScope.launch {
            observeSessionHistoryUseCase()
                // Without this the collector dies on the first throwable and the list silently
                // stops updating for the rest of the process, with nothing shown to the user. A
                // malformed row is the realistic trigger, via toSummary()'s requireNotNull checks.
                .catch { throwable -> Log.e(TAG, "Session history stopped updating", throwable) }
                .collect { summaries ->
                    latestSummaries = summaries
                    publish()
                }
        }
    }
}

fun SessionSummary.toRunSummaryUiModel(
    zoneId: ZoneId = ZoneId.systemDefault(),
): RunSummaryUiModel = RunSummaryUiModel(
    id = id,
    // A name the user chose replaces the timestamp: naming a run is for recognising it here.
    recordedAtLabel = title ?: formatRecordedAt(recordedAt, zoneId),
    distanceKm = formatDistanceKm(distanceMeters),
    durationLabel = formatDuration(durationMillis),
    paceLabel = formatPaceMinPerKm(averageSpeedMps),
    routePoints = routePolyline?.let(RoutePolyline::decode).orEmpty(),
)

private fun formatRecordedAt(recordedAtMillis: Long, zoneId: ZoneId): String =
    Instant.ofEpochMilli(recordedAtMillis).atZone(zoneId).format(recordedAtFormatter())

