package com.khiemnph.simpletracking.service

import android.app.Service
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.khiemnph.domain.fake.MockedLocationTrackingRepository
import com.khiemnph.domain.interactor.PauseSessionUseCase
import com.khiemnph.domain.interactor.RecordLocationFixUseCase
import com.khiemnph.domain.interactor.ResumeSessionUseCase
import com.khiemnph.domain.interactor.StopSessionUseCase
import com.khiemnph.domain.model.RawLocationFix
import com.khiemnph.domain.repository.LocationTrackingRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import javax.inject.Inject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config

/**
 * Exercises [TrackingService] through a real, Dagger-generated Hilt injection path -
 * `Hilt_TrackingService`'s `onCreate()`, not a manual field-assignment bypass - by running under
 * [HiltTestApplication] with [TestRepositoryModule][com.khiemnph.simpletracking.di.TestRepositoryModule]
 * and [TestUseCaseModule][com.khiemnph.simpletracking.di.TestUseCaseModule] swapping in in-memory
 * fakes and `mockk(relaxed = true)` use cases. The four injected use cases are still `mockk`
 * instances - only how they arrive at the Service's fields changes, from manual assignment to
 * real injection - so every `coVerify`-style interaction assertion below is unchanged from before
 * the retrofit.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [33])
class TrackingServiceTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val sessionId = "session-1"

    @Inject
    lateinit var pauseSessionUseCase: PauseSessionUseCase

    @Inject
    lateinit var resumeSessionUseCase: ResumeSessionUseCase

    @Inject
    lateinit var stopSessionUseCase: StopSessionUseCase

    @Inject
    lateinit var recordLocationFixUseCase: RecordLocationFixUseCase

    @Inject
    lateinit var locationTrackingRepository: LocationTrackingRepository

    private val fakeRepository: MockedLocationTrackingRepository
        get() = locationTrackingRepository as MockedLocationTrackingRepository

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    private fun fixFor(sessionId: String, timestamp: Long = 1_000L) = RawLocationFix(
        sessionId = sessionId,
        latitude = 10.0,
        longitude = 20.0,
        timestamp = timestamp,
        horizontalAccuracyMeters = 5f,
        speedMetersPerSec = 2f,
    )

    /**
     * Attaches a fresh [TrackingService] instance to [intent] and drives it through
     * [ServiceController.create] - the real `Hilt_TrackingService.onCreate()` -> `inject()` path -
     * then swaps [TrackingService.serviceScope] for one backed by [dispatcher] so use-case
     * dispatch can be verified deterministically via [runCurrent], rather than racing a real
     * background dispatcher. [TrackingService.serviceScope] is not an `@Inject` field, so
     * overwriting it after injection is safe and doesn't fight Hilt.
     */
    private fun launchService(dispatcher: TestDispatcher, intent: Intent): ServiceController<TrackingService> {
        val controller = ServiceController.of(TrackingService(), intent)
        controller.create()
        controller.get().serviceScope = CoroutineScope(SupervisorJob() + dispatcher)
        return controller
    }

    @Test
    fun givenLocationUpdatesFail_whenCollecting_thenNothingEscapesToTheServiceScope() = runTest {
        // Revoking location permission mid-session makes requestLocationUpdates throw inside the
        // callbackFlow. Uncaught on a SupervisorJob scope that reaches the thread's default
        // handler, which is a process crash and, with START_REDELIVER_INTENT, a crash loop.
        val dispatcher = StandardTestDispatcher(testScheduler)
        val escaped = mutableListOf<Throwable>()
        val controller = ServiceController.of(TrackingService(), TrackingService.startIntent(context, sessionId))
        controller.create()
        controller.get().serviceScope = CoroutineScope(
            SupervisorJob() + dispatcher + CoroutineExceptionHandler { _, throwable -> escaped.add(throwable) },
        )
        fakeRepository.failWith = SecurityException("ACCESS_FINE_LOCATION revoked")

        controller.get().onStartCommand(controller.intent, 0, 1)
        runCurrent()

        assertTrue("Location failures must not reach the service scope uncaught: $escaped", escaped.isEmpty())
    }

    @Test
    fun givenStartActionIntent_whenOnStartCommandCalled_thenServiceStartsForegroundWithMatchingNotificationId() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val controller = launchService(dispatcher, TrackingService.startIntent(context, sessionId))
        val service = controller.get()

        service.onStartCommand(controller.intent, 0, 1)

        assertEquals(TrackingNotificationFactory.NOTIFICATION_ID, shadowOf(service).lastForegroundNotificationId)
    }

    @Test
    fun givenAnyActionIntent_whenOnStartCommandCalled_thenReturnsStartRedeliverIntent() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val controller = launchService(dispatcher, TrackingService.startIntent(context, sessionId))
        val service = controller.get()

        val result = service.onStartCommand(controller.intent, 0, 1)

        assertEquals(Service.START_REDELIVER_INTENT, result)
    }

    @Test
    fun givenPauseActionIntent_whenOnStartCommandCalled_thenPauseSessionUseCaseInvokedWithCorrectSessionId() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val controller = launchService(dispatcher, TrackingService.pauseIntent(context, sessionId))
        val service = controller.get()

        service.onStartCommand(controller.intent, 0, 1)
        runCurrent()

        coVerify(exactly = 1) { pauseSessionUseCase(sessionId) }
    }

    @Test
    fun givenResumeActionIntent_whenOnStartCommandCalled_thenResumeSessionUseCaseInvokedWithCorrectSessionId() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val controller = launchService(dispatcher, TrackingService.resumeIntent(context, sessionId))
        val service = controller.get()

        service.onStartCommand(controller.intent, 0, 1)
        runCurrent()

        coVerify(exactly = 1) { resumeSessionUseCase(sessionId) }
    }

    @Test
    fun givenStopActionIntent_whenOnStartCommandCalled_thenStopSessionUseCaseInvokedAndServiceStops() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        coEvery { stopSessionUseCase(any()) } returns mockk(relaxed = true)
        val controller = launchService(dispatcher, TrackingService.stopIntent(context, sessionId))
        val service = controller.get()

        service.onStartCommand(controller.intent, 0, 1)
        runCurrent()

        coVerify(exactly = 1) { stopSessionUseCase(sessionId) }
        assertTrue(shadowOf(service).isStoppedBySelf)
    }

    @Test
    fun givenStopIntentWithoutThumbnailPath_whenOnStartCommandCalled_thenStopSessionUseCaseInvokedWithNull() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        coEvery { stopSessionUseCase(any()) } returns mockk(relaxed = true)
        val controller = launchService(dispatcher, TrackingService.stopIntent(context, sessionId))
        val service = controller.get()

        service.onStartCommand(controller.intent, 0, 1)
        runCurrent()

        coVerify(exactly = 1) { stopSessionUseCase(sessionId) }
    }

    @Test
    fun givenLocationTrackingRepositoryEmitsAFix_whenServiceRunning_thenRecordLocationFixUseCaseInvokedOnce() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val controller = launchService(dispatcher, TrackingService.startIntent(context, sessionId))
        val service = controller.get()
        service.onStartCommand(controller.intent, 0, 1)
        runCurrent()

        val fix = fixFor(sessionId)
        fakeRepository.emitFix(fix)
        runCurrent()

        coVerify(exactly = 1) { recordLocationFixUseCase(fix) }
    }

    @Test
    fun givenPauseActionIntent_whenOnStartCommandCalled_thenLocationCollectionJobIsCancelled() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val startIntent = TrackingService.startIntent(context, sessionId)
        val controller = launchService(dispatcher, startIntent)
        val service = controller.get()
        service.onStartCommand(startIntent, 0, 1)
        runCurrent()

        // A single Service instance is attached only once; further actions redeliver new intents
        // to the already-attached instance's onStartCommand, exactly as Android would.
        service.onStartCommand(TrackingService.pauseIntent(context, sessionId), 0, 2)
        runCurrent()

        fakeRepository.emitFix(fixFor(sessionId))
        runCurrent()

        coVerify(exactly = 0) { recordLocationFixUseCase(any()) }
    }

    @Test
    fun givenResumeAfterPause_whenOnStartCommandCalled_thenLocationCollectionResumes() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val startIntent = TrackingService.startIntent(context, sessionId)
        val controller = launchService(dispatcher, startIntent)
        val service = controller.get()
        service.onStartCommand(startIntent, 0, 1)
        runCurrent()
        service.onStartCommand(TrackingService.pauseIntent(context, sessionId), 0, 2)
        runCurrent()

        service.onStartCommand(TrackingService.resumeIntent(context, sessionId), 0, 3)
        runCurrent()
        val fix = fixFor(sessionId, timestamp = 2_000L)
        fakeRepository.emitFix(fix)
        runCurrent()

        coVerify(exactly = 1) { recordLocationFixUseCase(fix) }
    }

    @Test
    fun givenServiceDestroyed_whenLocationCollectionWasActive_thenNoFurtherFixesAreRecorded() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val controller = launchService(dispatcher, TrackingService.startIntent(context, sessionId))
        val service = controller.get()
        service.onStartCommand(controller.intent, 0, 1)
        runCurrent()

        service.onDestroy()
        fakeRepository.emitFix(fixFor(sessionId))
        runCurrent()

        coVerify(exactly = 0) { recordLocationFixUseCase(any()) }
    }

    @Test
    fun givenServiceKilledAndRestarted_whenStartActionRedelivered_thenLocationCollectionResumesInFreshInstance() = runTest {
        val redeliveredIntent = TrackingService.startIntent(context, sessionId)

        // First instance: starts collecting, then gets killed by the OS.
        val firstDispatcher = StandardTestDispatcher(testScheduler)
        val firstController = launchService(firstDispatcher, redeliveredIntent)
        firstController.get().onStartCommand(firstController.intent, 0, 1)
        runCurrent()
        firstController.destroy()

        // Fresh instance, same redelivered intent, no job of its own yet. It shares the same
        // injected LocationTrackingRepository singleton as the first instance did, since both
        // instances are created within this same test method's single Hilt component.
        val secondDispatcher = StandardTestDispatcher(testScheduler)
        val secondController = launchService(secondDispatcher, redeliveredIntent)
        secondController.get().onStartCommand(secondController.intent, 0, 1)
        runCurrent()

        val fix = fixFor(sessionId)
        fakeRepository.emitFix(fix)
        runCurrent()

        coVerify(exactly = 1) { recordLocationFixUseCase(fix) }
    }

    @Test
    fun givenServiceKilledAndRestarted_whenResumeActionRedelivered_thenServiceStartsForegroundInFreshInstance() = runTest {
        // First instance: starts, then pauses (a realistic pre-kill state), then gets killed by the OS.
        val firstDispatcher = StandardTestDispatcher(testScheduler)
        val firstController = launchService(firstDispatcher, TrackingService.startIntent(context, sessionId))
        firstController.get().onStartCommand(firstController.intent, 0, 1)
        runCurrent()
        firstController.get().onStartCommand(TrackingService.pauseIntent(context, sessionId), 0, 2)
        runCurrent()
        firstController.destroy()

        // Fresh instance: the OS redelivers the last-held intent, which is ACTION_RESUME - not
        // ACTION_START - since that's what the killed instance was last handling.
        val secondDispatcher = StandardTestDispatcher(testScheduler)
        val redeliveredResumeIntent = TrackingService.resumeIntent(context, sessionId)
        val secondController = launchService(secondDispatcher, redeliveredResumeIntent)

        secondController.get().onStartCommand(secondController.intent, 0, 1)
        runCurrent()

        assertEquals(
            TrackingNotificationFactory.NOTIFICATION_ID,
            shadowOf(secondController.get()).lastForegroundNotificationId,
        )
    }

    @Test
    fun givenNoAction_whenOnBindCalled_thenReturnsNull() {
        val dispatcher = StandardTestDispatcher()
        val controller = launchService(dispatcher, TrackingService.startIntent(context, sessionId))

        assertNull(controller.get().onBind(null))
    }
}
