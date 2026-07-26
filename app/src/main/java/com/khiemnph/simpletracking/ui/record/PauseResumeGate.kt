package com.khiemnph.simpletracking.ui.record

import com.khiemnph.domain.model.SessionStatus

/**
 * Bridges the gap between tapping Pause/Resume and the state emission that confirms it.
 *
 * A tap travels intent to Service to coroutine to Room write to invalidation to emission, which
 * takes about a second. Deciding the next command from the last *emitted* status during that
 * window has two costs: the button reads as dead, and a second tap re-sends the command already in
 * flight, which is the mechanism behind the duplicate writes in the pause/stop race.
 *
 * So the target status is held and displayed until reality catches up. Showing it optimistically
 * without holding it would flicker instead, because the once-per-second ticker keeps re-emitting
 * the old status until the write lands.
 *
 * Deliberately a plain class with no coroutines or clock of its own: the caller supplies elapsed
 * time, which is what makes every rule here directly testable.
 */
class PauseResumeGate {

    private var pendingStatus: SessionStatus? = null

    val isPending: Boolean get() = pendingStatus != null

    /**
     * Records a tap against [displayedStatus] (what the user was actually looking at, which may
     * itself be a pending value) and returns the status now being asked for.
     */
    fun onTapped(displayedStatus: SessionStatus): SessionStatus {
        val current = pendingStatus ?: displayedStatus
        val target = if (current == SessionStatus.PAUSED) SessionStatus.RUNNING else SessionStatus.PAUSED
        pendingStatus = target
        return target
    }

    /**
     * The status to show for an emission carrying [realStatus].
     *
     * [SessionStatus.STOPPED] is never masked: it is terminal, and hiding it behind a pause the
     * user asked for would leave a finished session looking live.
     */
    fun displayStatus(realStatus: SessionStatus): SessionStatus {
        val pending = pendingStatus ?: return realStatus
        if (realStatus == pending || realStatus == SessionStatus.STOPPED) {
            pendingStatus = null
            return realStatus
        }
        return pending
    }

    /** Releases the gate once a command has had long enough to land and clearly has not. */
    fun expireIfStale(elapsedSinceTapMillis: Long) {
        if (elapsedSinceTapMillis >= PENDING_TIMEOUT_MILLIS) pendingStatus = null
    }

    companion object {
        /**
         * Comfortably longer than the round trip, short enough that a genuinely lost command does
         * not leave the button stuck.
         */
        const val PENDING_TIMEOUT_MILLIS = 5_000L
    }
}
