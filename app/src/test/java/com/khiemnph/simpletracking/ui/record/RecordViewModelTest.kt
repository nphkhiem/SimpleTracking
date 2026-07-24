package com.khiemnph.simpletracking.ui.record

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.khiemnph.data.thumbnail.ThumbnailFileStore
import com.khiemnph.domain.fake.MockedSessionRepository
import com.khiemnph.domain.interactor.ObserveActiveSessionUseCase
import com.khiemnph.domain.interactor.StartSessionUseCase
import com.khiemnph.domain.model.LocationPoint
import com.khiemnph.domain.model.SessionStatus
import com.khiemnph.simpletracking.service.TrackingService
import com.khiemnph.simpletracking.util.EspressoIdlingResource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * [RecordViewModel] needs a real [Context] to build/send [TrackingService] intents, so this runs
 * under Robolectric rather than as a plain JVM unit test (matching
 * [com.khiemnph.simpletracking.service.TrackingServiceTest]'s reasoning for the same need).
 * [com.khiemnph.domain.interactor.StartSessionUseCase] is mocked so its invocation can be
 * interaction-verified; [ObserveActiveSessionUseCase] is wired to a real [MockedSessionRepository]
 * so state-based assertions (smoothing, status) reflect genuine emissions.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecordViewModelTest {

    private val repository = MockedSessionRepository()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val startSessionUseCase = mockk<StartSessionUseCase>()
    private val thumbnailFileStore = mockk<ThumbnailFileStore>()

    /**
     * A separate [CoroutineScope] instance from `viewModelScope`, deliberately not tied to
     * [Dispatchers.Main] - proves [RecordViewModel.onStopClicked] keeps working off of this scope
     * independently of whatever happens to `viewModelScope` (see
     * `givenViewModelClearedBeforeStopCallbackFires_...` below).
     */
    private val applicationScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher())

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = RecordViewModel(
        startSessionUseCase = startSessionUseCase,
        observeActiveSessionUseCase = ObserveActiveSessionUseCase(repository),
        thumbnailFileStore = thumbnailFileStore,
        context = context,
        applicationScope = applicationScope,
    )

    private fun nextStartedServiceIntent(): Intent? = shadowOf(context as Application).nextStartedService

    /**
     * [EspressoIdlingResource] is a JVM-wide singleton, so other tests in this class (or this
     * whole module, sharing one test JVM by default) can leave it in a non-idle state - e.g. a
     * [RecordViewModel.onPauseOrResumeClicked] increment with no matching emission ever following
     * it under this Robolectric fake, since nothing here runs a real [TrackingService] to produce
     * one. Draining first gives the exception-safety tests below a known-idle baseline to assert
     * against, independent of suite ordering.
     */
    private fun drainIdlingResourceToIdle() {
        var guard = 0
        while (!EspressoIdlingResource.countingIdlingResource.isIdleNow) {
            EspressoIdlingResource.decrement()
            guard++
            check(guard < 10_000) { "Failed to drain EspressoIdlingResource to idle before the test" }
        }
    }

    /** Forces [ContextCompat.startForegroundService]'s underlying call to fail for one specific
     * intent action, so [RecordViewModel]'s exception-safety `finally`/`catch` blocks around it can
     * be exercised deterministically without needing a real OS-level restriction to trigger. */
    private class ThrowingStartForegroundServiceContext(base: Context, private val throwForAction: String?) :
        ContextWrapper(base) {
        override fun startForegroundService(service: Intent): ComponentName? {
            if (service.action == throwForAction) throw IllegalStateException("Simulated startForegroundService failure")
            return super.startForegroundService(service)
        }
    }

    private fun point(sessionId: String, timestamp: Long, speedMetersPerSec: Float) = LocationPoint(
        sessionId = sessionId,
        latitude = 10.0 + timestamp * 0.0001,
        longitude = 20.0 + timestamp * 0.0001,
        timestamp = timestamp,
        horizontalAccuracyMeters = 5f,
        speedMetersPerSec = speedMetersPerSec,
    )

    /**
     * Compares action, target component, and every extra - not just [Intent.filterEquals]. Every
     * [TrackingService] intent extra is a `String`, so [Intent.getStringExtra] is enough here and
     * avoids the deprecated untyped [android.os.Bundle.get].
     */
    private fun assertSameIntent(expected: Intent, actual: Intent?) {
        assertNotNull("Expected a service to have been started, but none was", actual)
        assertEquals(expected.action, actual!!.action)
        assertEquals(expected.component, actual.component)
        val expectedKeys = expected.extras?.keySet().orEmpty()
        assertEquals(expectedKeys, actual.extras?.keySet().orEmpty())
        for (key in expectedKeys) {
            assertEquals(expected.getStringExtra(key), actual.getStringExtra(key))
        }
    }

    @Test
    fun givenNullExistingSessionId_whenResolveSessionCalled_thenStartSessionUseCaseInvokedAndServiceStartIntentSent() = runTest {
        coEvery { startSessionUseCase() } returns "new-session-id"
        val viewModel = createViewModel()

        viewModel.resolveSession(null)

        coVerify(exactly = 1) { startSessionUseCase() }
        assertSameIntent(TrackingService.startIntent(context, "new-session-id"), nextStartedServiceIntent())
    }

    @Test
    fun givenResolveSessionAlreadyCalled_whenCalledAgain_thenSecondCallIsANoOp() = runTest {
        coEvery { startSessionUseCase() } returns "new-session-id"
        val viewModel = createViewModel()

        viewModel.resolveSession(null)
        nextStartedServiceIntent() // drain the first call's start intent
        viewModel.resolveSession(null)

        coVerify(exactly = 1) { startSessionUseCase() }
        assertNull("A second resolveSession call must not send another start intent", nextStartedServiceIntent())
    }

    @Test
    fun givenNonNullExistingSessionId_whenResolveSessionCalled_thenStartSessionUseCaseNeverInvokedButServiceStartIntentStillSent() = runTest {
        val sessionId = repository.startSession()
        val viewModel = createViewModel()

        viewModel.resolveSession(sessionId)

        coVerify(exactly = 0) { startSessionUseCase() }
        assertSameIntent(TrackingService.startIntent(context, sessionId), nextStartedServiceIntent())
    }

    /**
     * Reads [RecordViewModel.uiState] synchronously rather than via Turbine's `awaitItem`: under
     * [UnconfinedTestDispatcher], every suspend call in this test body already completes fully
     * before returning, so `.value` always reflects the latest state. This matters here
     * specifically because a freshly-started session's very first computed [RecordUiState] can be
     * structurally equal to the [MutableStateFlow]'s initial default value (empty route, zero
     * speed/distance, a near-zero elapsed duration) - `StateFlow` conflates equal consecutive
     * values and would never deliver that as a distinct emission, making `awaitItem()` hang.
     */
    private fun RecordViewModel.state() = uiState.value

    @Test
    fun givenActiveSessionEmitsFourStatesWhereOnlyTwoHaveNewRoutePoints_whenUiStateCollected_thenSmoothingWindowOnlyUpdatesOnGenuinelyNewSamples() =
        runTest {
            val sessionId = repository.startSession()
            val viewModel = createViewModel()

            viewModel.resolveSession(sessionId)
            assertEquals(0f, viewModel.state().currentSpeedMps)

            repository.recordLocationPoint(point(sessionId, timestamp = 1_000L, speedMetersPerSec = 2f))
            assertEquals(2f, viewModel.state().currentSpeedMps)

            repository.recordLocationPoint(point(sessionId, timestamp = 2_000L, speedMetersPerSec = 4f))
            assertEquals(3f, viewModel.state().currentSpeedMps)

            // Ticker-only re-emission: no new route point, but a deliberately "poisoned"
            // currentSpeedMps of 99f standing in for a value that must NOT leak into the
            // smoothing window - proving the detection is keyed off route size, not this field.
            repository.emitTickerTick(sessionId, elapsedDurationMillis = 5_000L, currentSpeedMps = 99f)
            assertEquals(3f, viewModel.state().currentSpeedMps)
            assertEquals(5_000L, viewModel.state().elapsedDurationMillis)

            repository.recordLocationPoint(point(sessionId, timestamp = 3_000L, speedMetersPerSec = 6f))
            assertEquals(4f, viewModel.state().currentSpeedMps)
        }

    @Test
    fun givenExistingSessionAlreadyHasARoutePointBeforeObservingBegins_whenFirstStateEmitted_thenSmoothingWindowSeededImmediately() = runTest {
        val sessionId = repository.startSession()
        repository.recordLocationPoint(point(sessionId, timestamp = 1_000L, speedMetersPerSec = 7f))
        val viewModel = createViewModel()

        viewModel.resolveSession(sessionId)

        assertEquals(7f, viewModel.state().currentSpeedMps)
    }

    @Test
    fun givenActiveSessionState_whenUiStateCollected_thenDistanceDurationAverageSpeedAndRoutePassThroughUnsmoothed() = runTest {
        val sessionId = repository.startSession()
        val viewModel = createViewModel()
        viewModel.resolveSession(sessionId)

        repository.recordLocationPoint(point(sessionId, timestamp = 1_000L, speedMetersPerSec = 3f))
        val state = viewModel.state()

        assertEquals(1, state.route.size)
        assertEquals(3f, state.averageSpeedMps)
    }

    @Test
    fun givenPauseOrResumeClickedWhileRunning_whenInvoked_thenPauseIntentSentNotResumeIntent() = runTest {
        val sessionId = repository.startSession()
        val viewModel = createViewModel()
        viewModel.resolveSession(sessionId)
        nextStartedServiceIntent() // drain the start intent

        viewModel.onPauseOrResumeClicked()

        assertSameIntent(TrackingService.pauseIntent(context, sessionId), nextStartedServiceIntent())
    }

    @Test
    fun givenPauseOrResumeClickedWhilePaused_whenInvoked_thenResumeIntentSentNotPauseIntent() = runTest {
        val sessionId = repository.startSession()
        repository.pauseSession(sessionId)
        val viewModel = createViewModel()
        viewModel.resolveSession(sessionId)
        nextStartedServiceIntent() // drain the start intent

        viewModel.onPauseOrResumeClicked()

        assertSameIntent(TrackingService.resumeIntent(context, sessionId), nextStartedServiceIntent())
    }

    /**
     * Regression test for the resolveSession leak: [ContextCompat.startForegroundService]'s finally
     * block used to sit only inside `collect { }`, so an exception thrown before that loop is ever
     * reached (here, [startSessionUseCase] itself) left `resolveSession`'s own `increment()`
     * permanently unbalanced.
     *
     * The simulated failure is thrown from inside `viewModelScope.launch { }`, a coroutine scope
     * entirely separate from this test's own [runTest] scope, so it can only ever reach this test
     * as an *uncaught* coroutine exception - `kotlinx-coroutines-test` (correctly) treats any such
     * exception as a bug to report, attributing it to whichever [kotlinx.coroutines.test.TestScope]
     * is currently active, which is this test's own `runTest` block. The outer `try`/`catch` here
     * consumes that report itself, confined to this one test, instead of leaving it unhandled to
     * either fail this test outright or (if this were restructured to avoid `runTest` altogether)
     * leak into some later, unrelated `runTest`-based test as a stale
     * `UncaughtExceptionsBeforeTest` - confirmed empirically: an earlier version of this test that
     * dropped `runTest` to sidestep this exception did exactly that, failing a different,
     * innocent test instead.
     */
    @Test
    fun givenStartSessionUseCaseThrows_whenResolveSessionCalled_thenIdlingResourceIsBalancedNotLeaked() {
        drainIdlingResourceToIdle()
        coEvery { startSessionUseCase() } throws IllegalStateException("Simulated startSessionUseCase failure")
        val viewModel = createViewModel()

        val thrown = try {
            runTest { viewModel.resolveSession(null) }
            null
        } catch (e: IllegalStateException) {
            e
        }

        assertNotNull("Expected the simulated startSessionUseCase failure to surface", thrown)
        assertTrue(
            "Expected resolveSession's increment() to be balanced by a decrement even though " +
                "startSessionUseCase() threw before collect ever ran",
            EspressoIdlingResource.countingIdlingResource.isIdleNow,
        )
    }

    /**
     * Regression test for the onPauseOrResumeClicked leak: this function had no try/finally at all
     * around its [ContextCompat.startForegroundService] call, so an exception there (e.g. the OS's
     * background-start restrictions on a real device) left its `increment()` permanently
     * unbalanced, since only the *next* state emission was ever meant to balance it.
     *
     * Unlike `givenStartSessionUseCaseThrows_...` above, [RecordViewModel.onPauseOrResumeClicked]
     * doesn't launch a coroutine at all - its `startForegroundService` call is plain, synchronous
     * code, so the simulated failure below is an ordinary thrown exception this test can catch
     * directly with no `runTest`/`CoroutineExceptionHandler` involved.
     */
    @Test
    fun givenStartForegroundServiceThrows_whenPauseOrResumeClicked_thenIdlingResourceIsBalancedNotLeaked() {
        drainIdlingResourceToIdle()
        val sessionId = runBlocking { repository.startSession() }
        val throwingContext = ThrowingStartForegroundServiceContext(
            base = context,
            throwForAction = TrackingService.pauseIntent(context, sessionId).action,
        )
        val viewModel = RecordViewModel(
            startSessionUseCase = startSessionUseCase,
            observeActiveSessionUseCase = ObserveActiveSessionUseCase(repository),
            thumbnailFileStore = thumbnailFileStore,
            context = throwingContext,
            applicationScope = applicationScope,
        )
        viewModel.resolveSession(sessionId)
        assertTrue(
            "Expected resolveSession to leave the resource idle before exercising Pause",
            EspressoIdlingResource.countingIdlingResource.isIdleNow,
        )

        val thrown = try {
            viewModel.onPauseOrResumeClicked()
            null
        } catch (e: IllegalStateException) {
            e
        }

        assertNotNull("Expected the simulated startForegroundService failure to propagate", thrown)
        assertTrue(
            "Expected onPauseOrResumeClicked's increment() to be balanced by a decrement even " +
                "though startForegroundService() threw",
            EspressoIdlingResource.countingIdlingResource.isIdleNow,
        )
    }

    @Test
    fun givenPauseOrResumeClickedBeforeSessionResolved_whenInvoked_thenNoIntentSent() = runTest {
        val viewModel = createViewModel()

        viewModel.onPauseOrResumeClicked()

        assertNull(nextStartedServiceIntent())
    }

    @Test
    fun givenStopClickedWithBitmap_whenInvoked_thenThumbnailSavedThenStopIntentSentWithPath() = runTest {
        val sessionId = repository.startSession()
        val bitmap = mockk<Bitmap>()
        val thumbnailPath = "/data/thumbnails/$sessionId.png"
        coEvery { thumbnailFileStore.save(sessionId, bitmap) } returns thumbnailPath
        val viewModel = createViewModel()
        viewModel.resolveSession(sessionId)
        nextStartedServiceIntent() // drain the start intent

        viewModel.onStopClicked(bitmap)

        coVerify(exactly = 1) { thumbnailFileStore.save(sessionId, bitmap) }
        assertSameIntent(
            TrackingService.stopIntent(context, sessionId, thumbnailPath = thumbnailPath),
            nextStartedServiceIntent(),
        )
    }

    @Test
    fun givenStopClickedWithNullBitmap_whenInvoked_thenStopIntentSentWithoutPath() = runTest {
        val sessionId = repository.startSession()
        val viewModel = createViewModel()
        viewModel.resolveSession(sessionId)
        nextStartedServiceIntent() // drain the start intent

        viewModel.onStopClicked(null)

        coVerify(exactly = 0) { thumbnailFileStore.save(any(), any()) }
        assertSameIntent(TrackingService.stopIntent(context, sessionId), nextStartedServiceIntent())
    }

    /**
     * Regression test for the real-world race this fixes: [RecordFragment.handleStopClicked]
     * fires `googleMap.snapshot { ... }` (a genuinely async, rendering-dependent callback) and
     * pops the back stack immediately after, without waiting for it - destroying the Fragment and
     * clearing this ViewModel (cancelling `viewModelScope`) possibly before that callback ever
     * runs. [androidx.lifecycle.ViewModelStore.clear] is used here (not a mock, not reflection) to
     * trigger the exact same `onCleared()`/`viewModelScope` cancellation Android performs when a
     * ViewModel's owner is destroyed - then [RecordViewModel.onStopClicked] is invoked afterward,
     * simulating the snapshot callback firing late. If `onStopClicked` still launched on
     * `viewModelScope`, this coroutine would launch onto an already-cancelled Job and its body -
     * including sending the Stop intent - would silently never run, so this test would fail
     * without the [com.khiemnph.simpletracking.di.ApplicationScope] fix.
     */
    @Test
    fun givenViewModelClearedBeforeStopCallbackFires_whenStopClicked_thenStopIntentStillReachesTrackingService() = runTest {
        val sessionId = repository.startSession()
        val viewModel = createViewModel()
        viewModel.resolveSession(sessionId)
        nextStartedServiceIntent() // drain the start intent

        val viewModelStore = ViewModelStore()
        viewModelStore.put("record", viewModel)
        viewModelStore.clear() // mirrors popBackStack() destroying the Fragment/ViewModel

        viewModel.onStopClicked(null) // mirrors the snapshot callback firing after that teardown

        assertSameIntent(TrackingService.stopIntent(context, sessionId), nextStartedServiceIntent())
    }

    @Test
    fun givenNoSessionIdArgument_whenComponentBuilt_thenTargetsTrackingService() {
        // Regression guard for assertSameIntent's own correctness: both real intents genuinely
        // target TrackingService, so a bug that swapped in the wrong service class would fail
        // the interaction tests above for the right reason.
        val expectedComponent = ComponentName(context, TrackingService::class.java)
        assertEquals(expectedComponent, TrackingService.startIntent(context, "x").component)
    }
}
