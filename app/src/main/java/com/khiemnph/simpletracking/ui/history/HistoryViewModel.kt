package com.khiemnph.simpletracking.ui.history

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khiemnph.domain.interactor.ObserveSessionHistoryUseCase
import com.khiemnph.domain.model.SessionSummary
import com.khiemnph.domain.util.RoutePolyline
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

private const val METERS_PER_KILOMETER = 1_000.0
private const val SECONDS_PER_MILLIS_DIVISOR = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L
private const val MPS_TO_KMH_FACTOR = 3.6f
private val RECORDED_AT_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, h:mm a", Locale.US)

/**
 * Exposes [ObserveSessionHistoryUseCase]'s session summaries as pre-formatted
 * [HistorySummaryUiModel]s for [HistoryFragment]/[HistoryAdapter]. Ordering is pass-through: the
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
private const val TAG = "HistoryViewModel"

@HiltViewModel
class HistoryViewModel @Inject constructor(
    observeSessionHistoryUseCase: ObserveSessionHistoryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<List<HistorySummaryUiModel>>(emptyList())
    val uiState: StateFlow<List<HistorySummaryUiModel>> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSessionHistoryUseCase()
                // Without this the collector dies on the first throwable and the list silently
                // stops updating for the rest of the process, with nothing shown to the user. A
                // malformed row is the realistic trigger, via toSummary()'s requireNotNull checks.
                .catch { throwable -> Log.e(TAG, "Session history stopped updating", throwable) }
                .collect { summaries ->
                    _uiState.value = summaries.map { it.toHistorySummaryUiModel() }
                }
        }
    }
}

fun SessionSummary.toHistorySummaryUiModel(
    zoneId: ZoneId = ZoneId.systemDefault(),
): HistorySummaryUiModel = HistorySummaryUiModel(
    id = id,
    recordedAtLabel = formatRecordedAt(recordedAt, zoneId),
    distanceLabel = formatDistanceKm(distanceMeters),
    durationLabel = formatDuration(durationMillis),
    averageSpeedLabel = formatAverageSpeedKmh(averageSpeedMps),
    routePoints = routePolyline?.let(RoutePolyline::decode).orEmpty(),
)

private fun formatRecordedAt(recordedAtMillis: Long, zoneId: ZoneId): String =
    Instant.ofEpochMilli(recordedAtMillis).atZone(zoneId).format(RECORDED_AT_FORMATTER)

private fun formatDistanceKm(distanceMeters: Double): String =
    String.format(Locale.US, "%.2f km", distanceMeters / METERS_PER_KILOMETER)

private fun formatDuration(durationMillis: Long): String {
    val totalSeconds = durationMillis / SECONDS_PER_MILLIS_DIVISOR
    val hours = totalSeconds / SECONDS_PER_HOUR
    val minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

private fun formatAverageSpeedKmh(averageSpeedMps: Float): String =
    String.format(Locale.US, "%.1f km/h avg", averageSpeedMps * MPS_TO_KMH_FACTOR)
