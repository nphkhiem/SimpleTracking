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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TrackingServiceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val sessionId = "session-1"

    private val pauseSessionUseCase: PauseSessionUseCase = mockk(relaxed = true)
    private val resumeSessionUseCase: ResumeSessionUseCase = mockk(relaxed = true)
    private val stopSessionUseCase: StopSessionUseCase = mockk(relaxed = true)
    private val recordLocationFixUseCase: RecordLocationFixUseCase = mockk(relaxed = true)

    private fun fixFor(sessionId: String, timestamp: Long = 1_000L) = RawLocationFix(
        sessionId = sessionId,
        latitude = 10.0,
        longitude = 20.0,
        timestamp = timestamp,
        horizontalAccuracyMeters = 5f,
        speedMetersPerSec = 2f,
    )

    /** Builds an un-created (no onCreate/Hilt injection) but attached Service instance with
     * fields set directly - this test deliberately bypasses real Hilt injection: fields are set
     * manually and the coroutine scope is swapped for one backed by [dispatcher] so use-case
     * dispatch can be verified deterministically via [runCurrent], rather than exercising the
     * real Dagger graph or racing a background dispatcher. */
    private fun buildService(
        dispatcher: TestDispatcher,
        locationTrackingRepository: MockedLocationTrackingRepository = MockedLocationTrackingRepository(),
    ): TrackingService {
        val service = TrackingService()
        service.pauseSessionUseCase = pauseSessionUseCase
        service.resumeSessionUseCase = resumeSessionUseCase
        service.stopSessionUseCase = stopSessionUseCase
        service.recordLocationFixUseCase = recordLocationFixUseCase
        service.locationTrackingRepository = locationTrackingRepository
        service.notificationFactory = TrackingNotificationFactory(context)
        service.serviceScope = CoroutineScope(SupervisorJob() + dispatcher)
        return service
    }

    private fun attach(service: TrackingService, intent: Intent): ServiceController<TrackingService> =
        ServiceController.of(service, intent)

    @Test
    fun givenStartActionIntent_whenOnStartCommandCalled_thenServiceStartsForegroundWithMatchingNotificationId() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val service = buildService(dispatcher)
        val controller = attach(service, TrackingService.startIntent(context, sessionId))

        service.onStartCommand(controller.intent, 0, 1)

        assertEquals(TrackingNotificationFactory.NOTIFICATION_ID, shadowOf(service).lastForegroundNotificationId)
    }

    @Test
    fun givenAnyActionIntent_whenOnStartCommandCalled_thenReturnsStartRedeliverIntent() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val service = buildService(dispatcher)
        val controller = attach(service, TrackingService.startIntent(context, sessionId))

        val result = service.onStartCommand(controller.intent, 0, 1)

        assertEquals(Service.START_REDELIVER_INTENT, result)
    }

    @Test
    fun givenPauseActionIntent_whenOnStartCommandCalled_thenPauseSessionUseCaseInvokedWithCorrectSessionId() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val service = buildService(dispatcher)
        val controller = attach(service, TrackingService.pauseIntent(context, sessionId))

        service.onStartCommand(controller.intent, 0, 1)
        runCurrent()

        coVerify(exactly = 1) { pauseSessionUseCase(sessionId) }
    }

    @Test
    fun givenResumeActionIntent_whenOnStartCommandCalled_thenResumeSessionUseCaseInvokedWithCorrectSessionId() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val service = buildService(dispatcher)
        val controller = attach(service, TrackingService.resumeIntent(context, sessionId))

        service.onStartCommand(controller.intent, 0, 1)
        runCurrent()

        coVerify(exactly = 1) { resumeSessionUseCase(sessionId) }
    }

    @Test
    fun givenStopActionIntent_whenOnStartCommandCalled_thenStopSessionUseCaseInvokedWithNullThumbnailAndServiceStops() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        coEvery { stopSessionUseCase(any(), any()) } returns mockk(relaxed = true)
        val service = buildService(dispatcher)
        val controller = attach(service, TrackingService.stopIntent(context, sessionId))

        service.onStartCommand(controller.intent, 0, 1)
        runCurrent()

        coVerify(exactly = 1) { stopSessionUseCase(sessionId, null) }
        assertTrue(shadowOf(service).isStoppedBySelf)
    }

    @Test
    fun givenLocationTrackingRepositoryEmitsAFix_whenServiceRunning_thenRecordLocationFixUseCaseInvokedOnce() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val fakeRepository = MockedLocationTrackingRepository()
        val service = buildService(dispatcher, fakeRepository)
        val controller = attach(service, TrackingService.startIntent(context, sessionId))
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
        val fakeRepository = MockedLocationTrackingRepository()
        val service = buildService(dispatcher, fakeRepository)
        val startIntent = TrackingService.startIntent(context, sessionId)
        attach(service, startIntent)
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
        val fakeRepository = MockedLocationTrackingRepository()
        val service = buildService(dispatcher, fakeRepository)
        val startIntent = TrackingService.startIntent(context, sessionId)
        attach(service, startIntent)
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
        val fakeRepository = MockedLocationTrackingRepository()
        val service = buildService(dispatcher, fakeRepository)
        val controller = attach(service, TrackingService.startIntent(context, sessionId))
        service.onStartCommand(controller.intent, 0, 1)
        runCurrent()

        service.onDestroy()
        fakeRepository.emitFix(fixFor(sessionId))
        runCurrent()

        coVerify(exactly = 0) { recordLocationFixUseCase(any()) }
    }

    @Test
    fun givenServiceKilledAndRestarted_whenStartActionRedelivered_thenLocationCollectionResumesInFreshInstance() = runTest {
        val fakeRepository = MockedLocationTrackingRepository()
        val redeliveredIntent = TrackingService.startIntent(context, sessionId)

        // First instance: starts collecting, then gets killed by the OS.
        val firstDispatcher = StandardTestDispatcher(testScheduler)
        val firstService = buildService(firstDispatcher, fakeRepository)
        val firstController = attach(firstService, redeliveredIntent)
        firstService.onStartCommand(firstController.intent, 0, 1)
        runCurrent()
        firstController.destroy()

        // Fresh instance, same redelivered intent, no job of its own yet.
        val secondDispatcher = StandardTestDispatcher(testScheduler)
        val secondService = buildService(secondDispatcher, fakeRepository)
        val secondController = attach(secondService, redeliveredIntent)
        secondService.onStartCommand(secondController.intent, 0, 1)
        runCurrent()

        val fix = fixFor(sessionId)
        fakeRepository.emitFix(fix)
        runCurrent()

        coVerify(exactly = 1) { recordLocationFixUseCase(fix) }
    }

    @Test
    fun givenServiceKilledAndRestarted_whenResumeActionRedelivered_thenServiceStartsForegroundInFreshInstance() = runTest {
        val fakeRepository = MockedLocationTrackingRepository()

        // First instance: starts, then pauses (a realistic pre-kill state), then gets killed by the OS.
        val firstDispatcher = StandardTestDispatcher(testScheduler)
        val firstService = buildService(firstDispatcher, fakeRepository)
        val firstController = attach(firstService, TrackingService.startIntent(context, sessionId))
        firstService.onStartCommand(firstController.intent, 0, 1)
        runCurrent()
        firstService.onStartCommand(TrackingService.pauseIntent(context, sessionId), 0, 2)
        runCurrent()
        firstController.destroy()

        // Fresh instance: the OS redelivers the last-held intent, which is ACTION_RESUME - not
        // ACTION_START - since that's what the killed instance was last handling.
        val secondDispatcher = StandardTestDispatcher(testScheduler)
        val secondService = buildService(secondDispatcher, fakeRepository)
        val redeliveredResumeIntent = TrackingService.resumeIntent(context, sessionId)
        val secondController = attach(secondService, redeliveredResumeIntent)

        secondService.onStartCommand(secondController.intent, 0, 1)
        runCurrent()

        assertEquals(TrackingNotificationFactory.NOTIFICATION_ID, shadowOf(secondService).lastForegroundNotificationId)
    }

    @Test
    fun givenNoAction_whenOnBindCalled_thenReturnsNull() {
        val dispatcher = StandardTestDispatcher()
        val service = buildService(dispatcher)

        assertNull(service.onBind(null))
    }
}
