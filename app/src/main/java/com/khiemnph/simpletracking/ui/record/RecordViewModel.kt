package com.khiemnph.simpletracking.ui.record

import android.content.Context
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khiemnph.domain.interactor.ObserveActiveSessionUseCase
import com.khiemnph.domain.interactor.StartSessionUseCase
import com.khiemnph.domain.model.ActiveSessionState
import com.khiemnph.domain.model.SessionStatus
import com.khiemnph.simpletracking.di.ApplicationScope
import com.khiemnph.simpletracking.service.TrackingService
import com.khiemnph.simpletracking.util.EspressoIdlingResource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
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
    @ApplicationContext private val context: Context,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    private var sessionId: String? = null

    /** True once [resolveSession] has resolved a session id - lets [RecordFragment] skip
     * re-running its new-session permission/Location-Service flow on every `onViewCreated` (e.g.
     * across a configuration change), since this [ViewModel] - unlike the Fragment/View - survives
     * that recreation. */
    val hasResolvedSession: Boolean
        get() = sessionId != null

    /**
     * Presentation-only rolling window of the last few genuinely-new [ActiveSessionState.currentSpeedMps]
     * samples - see [isGenuinelyNewSample].
     */
    private val currentSpeedWindow = ArrayDeque<Float>(CURRENT_SPEED_WINDOW_SIZE)
    private var previousState: ActiveSessionState? = null

    /** Holds the status the user asked for until the write confirming it comes back. */
    private val pauseResumeGate = PauseResumeGate()
    private var pauseResumeTapElapsedRealtime = 0L

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
        EspressoIdlingResource.increment()
        viewModelScope.launch {
            // reachedFirstEmission tracks whether collect's own per-emission finally below has
            // taken over balancing this method's increment. If startSessionUseCase() or
            // startForegroundService throws before collect ever runs - or the coroutine is
            // cancelled while still suspended waiting for that first emission - this outer finally
            // is the only thing left to balance it, or the increment would leak forever.
            var reachedFirstEmission = false
            try {
                val id = existingSessionId ?: startSessionUseCase()
                sessionId = id
                ContextCompat.startForegroundService(context, TrackingService.startIntent(context, id))
                observeActiveSessionUseCase().collect { state ->
                    reachedFirstEmission = true
                    try {
                        if (state != null) updateUiState(state)
                    } finally {
                        // Balances this call's own increment above, plus one increment per
                        // subsequent state-changing action (onPauseOrResumeClicked, or a test
                        // feeding a fix straight through the fake LocationTrackingRepository) -
                        // every mutation this app makes to the active session's state ultimately
                        // surfaces as exactly one emission here, which is what makes Espresso's
                        // IdlingRegistry check reliably wait until this Flow-driven UI has
                        // actually caught up, instead of racing the async hop off
                        // TrackingService's background dispatcher.
                        EspressoIdlingResource.decrement()
                    }
                }
            } finally {
                if (!reachedFirstEmission) EspressoIdlingResource.decrement()
            }
        }
    }

    fun onPauseOrResumeClicked() {
        val id = sessionId ?: return
        EspressoIdlingResource.increment()
        try {
            val target = pauseResumeGate.onTapped(_uiState.value.status)
            pauseResumeTapElapsedRealtime = SystemClock.elapsedRealtime()
            // Reflect the tap immediately. The gate keeps this showing until the write lands, so
            // the once-per-second ticker cannot flicker it back to the pre-tap status meanwhile.
            _uiState.value = _uiState.value.copy(status = target)
            val intent = if (target == SessionStatus.RUNNING) {
                TrackingService.resumeIntent(context, id)
            } else {
                TrackingService.pauseIntent(context, id)
            }
            ContextCompat.startForegroundService(context, intent)
        } catch (e: Exception) {
            // Not a plain `finally`: on success this increment is balanced by the state emission
            // TrackingService's async pause/resume handling eventually produces (see
            // resolveSession's collect above), not by this function returning. Only compensate
            // here when startForegroundService throws before ever reaching TrackingService, since
            // then no such emission will ever arrive to balance it.
            EspressoIdlingResource.decrement()
            throw e
        }
    }

    /**
     * Launched on [applicationScope], not `viewModelScope`, deliberately: the Fragment pops the
     * back stack as it stops, which destroys the Fragment and clears this ViewModel. On
     * `viewModelScope` the coroutine would be cancelled mid-flight, the Stop intent would never
     * reach [TrackingService], and tracking would keep running after the user believed they had
     * stopped it.
     */
    fun onStopClicked() {
        val id = sessionId ?: return
        applicationScope.launch {
            ContextCompat.startForegroundService(context, TrackingService.stopIntent(context, id))
        }
    }

    private fun updateUiState(state: ActiveSessionState) {
        if (isGenuinelyNewSample(state)) {
            if (currentSpeedWindow.size == CURRENT_SPEED_WINDOW_SIZE) currentSpeedWindow.removeFirst()
            currentSpeedWindow.addLast(state.currentSpeedMps)
        }
        previousState = state

        pauseResumeGate.expireIfStale(SystemClock.elapsedRealtime() - pauseResumeTapElapsedRealtime)

        _uiState.value = RecordUiState(
            status = pauseResumeGate.displayStatus(state.session.status),
            distanceMeters = state.distanceMeters,
            elapsedDurationMillis = state.elapsedDurationMillis,
            currentSpeedMps = if (currentSpeedWindow.isEmpty()) 0f else currentSpeedWindow.average().toFloat(),
            averageSpeedMps = state.averageSpeedMps,
            route = state.route,
            gpsSignal = state.gpsSignal,
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
