package com.khiemnph.simpletracking.ui.record

import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khiemnph.data.thumbnail.ThumbnailFileStore
import com.khiemnph.domain.interactor.ObserveActiveSessionUseCase
import com.khiemnph.domain.interactor.StartSessionUseCase
import com.khiemnph.domain.model.ActiveSessionState
import com.khiemnph.domain.model.SessionStatus
import com.khiemnph.simpletracking.service.TrackingService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val CURRENT_SPEED_WINDOW_SIZE = 4

/**
 * Drives [RecordFragment]'s live-tracking screen. Deliberately never calls
 * [com.khiemnph.domain.interactor.PauseSessionUseCase]/[com.khiemnph.domain.interactor.ResumeSessionUseCase]/
 * [com.khiemnph.domain.interactor.StopSessionUseCase] directly - every state transition after the
 * session exists is sent to [TrackingService] as an intent, exactly as the Service's own
 * notification actions do, so the Service stays the single place those use cases are invoked from
 * regardless of trigger source. [StartSessionUseCase] is the one exception: creating a brand-new
 * session is a one-time action this ViewModel performs itself before telling the Service to start
 * collecting for that id.
 */
@HiltViewModel
class RecordViewModel @Inject constructor(
    private val startSessionUseCase: StartSessionUseCase,
    private val observeActiveSessionUseCase: ObserveActiveSessionUseCase,
    private val thumbnailFileStore: ThumbnailFileStore,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    private var sessionId: String? = null

    /**
     * Presentation-only rolling window of the last few genuinely-new [ActiveSessionState.currentSpeedMps]
     * samples - see [isGenuinelyNewSample].
     */
    private val currentSpeedWindow = ArrayDeque<Float>(CURRENT_SPEED_WINDOW_SIZE)
    private var previousState: ActiveSessionState? = null

    /**
     * Resolves which session this screen tracks, then starts (or re-affirms) Service collection
     * for it and begins observing its live state. Must only be called once the Fragment has
     * already resolved any required permission check for the "brand-new session" case; the
     * "resume an existing session" case ([existingSessionId] non-null) skips that check entirely.
     *
     * A no-op if a session was already resolved - [RecordFragment] calls this from
     * `onViewCreated`, which re-runs after the view (not the ViewModel) is recreated across a
     * configuration change, and re-collecting [observeActiveSessionUseCase] a second time would
     * race two collectors against the same mutable smoothing state.
     */
    fun resolveSession(existingSessionId: String?) {
        if (sessionId != null) return
        viewModelScope.launch {
            val id = existingSessionId ?: startSessionUseCase()
            sessionId = id
            ContextCompat.startForegroundService(context, TrackingService.startIntent(context, id))
            observeActiveSessionUseCase().collect { state ->
                if (state != null) updateUiState(state)
            }
        }
    }

    fun onPauseOrResumeClicked() {
        val id = sessionId ?: return
        val intent = if (_uiState.value.status == SessionStatus.PAUSED) {
            TrackingService.resumeIntent(context, id)
        } else {
            TrackingService.pauseIntent(context, id)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    /**
     * [bitmap] is a best-effort map snapshot the Fragment attempts before calling this - `null`
     * when the map wasn't ready/available, which simply results in the Stop intent carrying no
     * thumbnail path (a normal placeholder case in History, not an error).
     */
    fun onStopClicked(bitmap: Bitmap?) {
        val id = sessionId ?: return
        viewModelScope.launch {
            val thumbnailPath = bitmap?.let { thumbnailFileStore.save(id, it) }
            ContextCompat.startForegroundService(context, TrackingService.stopIntent(context, id, thumbnailPath))
        }
    }

    private fun updateUiState(state: ActiveSessionState) {
        if (isGenuinelyNewSample(state)) {
            if (currentSpeedWindow.size == CURRENT_SPEED_WINDOW_SIZE) currentSpeedWindow.removeFirst()
            currentSpeedWindow.addLast(state.currentSpeedMps)
        }
        previousState = state

        _uiState.value = RecordUiState(
            status = state.session.status,
            distanceMeters = state.distanceMeters,
            elapsedDurationMillis = state.elapsedDurationMillis,
            currentSpeedMps = if (currentSpeedWindow.isEmpty()) 0f else currentSpeedWindow.average().toFloat(),
            averageSpeedMps = state.averageSpeedMps,
            route = state.route,
        )
    }

    /**
     * The underlying repository re-emits [ActiveSessionState] once per second via a ticker purely
     * to advance [ActiveSessionState.elapsedDurationMillis], even with no new GPS fix - and
     * [ActiveSessionState.currentSpeedMps] stays byte-for-byte identical between real fixes. Since
     * route points are only ever appended, never replaced, a change in route size (or, for the
     * very first state this ViewModel ever observes, a non-empty route already present - e.g.
     * resuming a session that already had fixes recorded before this screen started observing it)
     * is what distinguishes a genuinely new sample from a ticker-only re-emission; this is checked
     * against route size rather than [ActiveSessionState.currentSpeedMps] itself so the check
     * still works correctly even in the (real-world impossible, but worth guarding) case where a
     * ticker re-emission's speed value happened to differ.
     */
    private fun isGenuinelyNewSample(state: ActiveSessionState): Boolean {
        val previous = previousState ?: return state.route.isNotEmpty()
        return previous.route.size != state.route.size
    }
}
