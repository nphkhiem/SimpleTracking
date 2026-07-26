package com.khiemnph.simpletracking.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.app.ServiceCompat
import com.khiemnph.domain.interactor.PauseSessionUseCase
import com.khiemnph.domain.interactor.RecordLocationFixUseCase
import com.khiemnph.domain.interactor.ResumeSessionUseCase
import com.khiemnph.domain.interactor.StopSessionUseCase
import com.khiemnph.domain.repository.LocationTrackingRepository
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val TAG = "TrackingService"

/**
 * Last line of defence for work launched on the service scope. A [SupervisorJob] stops one failing
 * child cancelling its siblings, but it does not swallow the failure: without a handler it reaches
 * the thread's default uncaught handler and kills the process.
 */
private fun serviceExceptionHandler() = CoroutineExceptionHandler { _, throwable ->
    Log.e(TAG, "Unhandled failure on the tracking service scope", throwable)
}

/**
 * Thin foreground GPS collector: holds zero business logic of its own. It only starts/stops GPS
 * collection and routes Pause/Resume/Stop commands - from both in-app UI and the notification's
 * own action buttons - into the exact same domain use cases. Idempotency, validation, and all
 * math already live in `:domain`.
 */
@AndroidEntryPoint
class TrackingService : Service() {

    @Inject lateinit var recordLocationFixUseCase: RecordLocationFixUseCase
    @Inject lateinit var pauseSessionUseCase: PauseSessionUseCase
    @Inject lateinit var resumeSessionUseCase: ResumeSessionUseCase
    @Inject lateinit var stopSessionUseCase: StopSessionUseCase
    @Inject lateinit var locationTrackingRepository: LocationTrackingRepository
    @Inject lateinit var notificationFactory: TrackingNotificationFactory

    /**
     * Test seam: overridden with a [kotlinx.coroutines.test.TestScope]-backed scope in unit tests
     * so use-case dispatch can be verified deterministically instead of racing a real background
     * dispatcher. Production always uses the default value.
     */
    internal var serviceScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default + serviceExceptionHandler())

    private var locationCollectionJob: Job? = null
    private var currentSessionId: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        destroyLatch = CountDownLatch(1)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sessionId = intent?.getStringExtra(EXTRA_SESSION_ID)
        if (sessionId != null) {
            promoteToForeground(sessionId)
            when (intent.action) {
                ACTION_START -> handleStart(sessionId)
                ACTION_PAUSE -> handlePause(sessionId)
                ACTION_RESUME -> handleResume(sessionId)
                ACTION_STOP -> handleStop(sessionId)
            }
        }
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
        destroyLatch.countDown()
    }

    /**
     * Registers this Service as a foreground service, unconditionally, before any action is
     * dispatched below. This must fire for every action - not only ACTION_START - because this
     * Service returns START_REDELIVER_INTENT and never calls stopSelf(startId), so the *last
     * delivered* intent the OS holds for redelivery-after-kill can just as easily be ACTION_PAUSE
     * or ACTION_RESUME as ACTION_START. A freshly-created instance redelivered, say, ACTION_RESUME
     * must still promote itself to the foreground, or it's vulnerable to background-execution
     * limits immediately after restarting collection. ServiceCompat.startForeground is safe to
     * call repeatedly per Android docs; handlePause/handleResume refine the notification's
     * paused/running content afterward.
     */
    private fun promoteToForeground(sessionId: String) {
        val notification = notificationFactory.buildNotification(sessionId, isPaused = false)
        ServiceCompat.startForeground(
            this,
            TrackingNotificationFactory.NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
        )
    }

    private fun handleStart(sessionId: String) {
        startCollectingIfNeeded(sessionId)
    }

    private fun handlePause(sessionId: String) {
        stopCollecting()
        serviceScope.launch { pauseSessionUseCase(sessionId) }
        notificationFactory.updateNotification(sessionId, isPaused = true)
    }

    private fun handleResume(sessionId: String) {
        serviceScope.launch { resumeSessionUseCase(sessionId) }
        startCollectingIfNeeded(sessionId)
        notificationFactory.updateNotification(sessionId, isPaused = false)
    }

    private fun handleStop(sessionId: String) {
        stopCollecting()
        currentSessionId = null
        serviceScope.launch {
            stopSessionUseCase(sessionId)
            stopSelf()
        }
    }

    /**
     * Starts collecting GPS fixes for [sessionId] unless a job for that same session is already
     * active - this is what makes redelivery-after-kill safe: a fresh Service instance always has
     * a null job, so redelivered START/RESUME intents naturally (re)start collection.
     */
    private fun startCollectingIfNeeded(sessionId: String) {
        if (currentSessionId == sessionId && locationCollectionJob?.isActive == true) return
        locationCollectionJob?.cancel()
        currentSessionId = sessionId
        locationCollectionJob = locationTrackingRepository.locationUpdates(sessionId)
            .onEach { fix -> recordLocationFixUseCase(fix) }
            // The provider can fail at any time - a revoked location permission throws
            // SecurityException from inside the callbackFlow, and persisting a fix can fail on a
            // full disk. Uncaught, either reaches the thread's default handler and kills the
            // process; START_REDELIVER_INTENT then replays the intent into a fresh instance that
            // fails the same way, which is a crash loop. Collection stops, the session does not.
            .catch { throwable -> Log.e(TAG, "Location collection failed for session $sessionId", throwable) }
            .launchIn(serviceScope)
    }

    private fun stopCollecting() {
        locationCollectionJob?.cancel()
        locationCollectionJob = null
    }

    companion object {
        private const val ACTION_START = "com.khiemnph.simpletracking.action.START"
        private const val ACTION_PAUSE = "com.khiemnph.simpletracking.action.PAUSE"
        private const val ACTION_RESUME = "com.khiemnph.simpletracking.action.RESUME"
        private const val ACTION_STOP = "com.khiemnph.simpletracking.action.STOP"
        private const val EXTRA_SESSION_ID = "com.khiemnph.simpletracking.extra.SESSION_ID"

        /**
         * Counted down by [onDestroy] and replaced with a fresh, un-counted latch by [onCreate] -
         * a companion-level (not per-instance) field because instrumented tests calling
         * [Context.stopService] have no reference to the live [TrackingService] instance to await
         * on directly. Starts pre-counted-down so [awaitDestroyed] is a no-op if no instance was
         * ever created.
         */
        @Volatile
        private var destroyLatch = CountDownLatch(0)

        /**
         * Test-only synchronization point for instrumented-test teardown: [Context.stopService]
         * only *requests* a stop and returns immediately, well before [onDestroy] actually runs on
         * the main thread, so a test that calls `stopService()` and returns without waiting for
         * this can race the next test method's own `startForegroundService` call against this
         * still-tearing-down instance. Call this right after `stopService()` in `@After` to block
         * until `onDestroy` has genuinely finished. Returns `false` (rather than throwing) on
         * timeout so the caller can turn it into a clear test assertion failure instead of a
         * confusing hang.
         */
        @VisibleForTesting
        fun awaitDestroyed(timeoutMillis: Long = 5_000L): Boolean =
            destroyLatch.await(timeoutMillis, TimeUnit.MILLISECONDS)

        fun startIntent(context: Context, sessionId: String): Intent = intentFor(context, ACTION_START, sessionId)

        fun pauseIntent(context: Context, sessionId: String): Intent = intentFor(context, ACTION_PAUSE, sessionId)

        fun resumeIntent(context: Context, sessionId: String): Intent = intentFor(context, ACTION_RESUME, sessionId)

        /**
         * Carries nothing but the session id. The route's shape is derived from what was recorded,
         * inside `StopSessionUseCase`, so every Stop path behaves identically whether it came from
         * the screen or the notification's own action.
         */
        fun stopIntent(context: Context, sessionId: String): Intent =
            intentFor(context, ACTION_STOP, sessionId)

        private fun intentFor(context: Context, action: String, sessionId: String): Intent =
            Intent(context, TrackingService::class.java).apply {
                this.action = action
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
    }
}
