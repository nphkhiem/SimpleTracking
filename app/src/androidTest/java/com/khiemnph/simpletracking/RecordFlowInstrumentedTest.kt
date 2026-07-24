package com.khiemnph.simpletracking

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.khiemnph.data.di.RepositoryModule
import com.khiemnph.data.util.Clock
import com.khiemnph.data.util.WallClock
import com.khiemnph.domain.fake.MockedLocationTrackingRepository
import com.khiemnph.domain.fake.MockedSessionRepository
import com.khiemnph.domain.model.RawLocationFix
import com.khiemnph.domain.model.Session
import com.khiemnph.domain.model.SessionStatus
import com.khiemnph.domain.repository.LocationTrackingRepository
import com.khiemnph.domain.repository.SessionRepository
import com.khiemnph.simpletracking.service.TrackingService
import com.khiemnph.simpletracking.ui.MainActivity
import com.khiemnph.simpletracking.util.EspressoIdlingResource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises [com.khiemnph.simpletracking.ui.record.RecordFragment] end-to-end on a real device:
 * a real Fragment transaction from [com.khiemnph.simpletracking.ui.history.HistoryFragment], a
 * real [com.khiemnph.simpletracking.service.TrackingService] instance actually started via
 * `startForegroundService` and dispatching through `onStartCommand`, and a real cross-thread hop
 * from that Service's background dispatcher back to the Main-thread UI - none of which
 * [com.khiemnph.simpletracking.ui.record.RecordFragmentTest]'s Robolectric harness can reach,
 * since nothing there ever actually runs the Service (see that test class's own docs).
 *
 * [com.google.android.gms.maps.GoogleMap]'s marker objects aren't practically assertable through
 * Espresso, so "map shows start marker" is instead verified via the fake [SessionRepository]'s own
 * state and [com.khiemnph.simpletracking.ui.record.RecordFragment]'s rendered UI - both direct,
 * reliable proxies for "the session actually started and a route point was accepted."
 *
 * [tearDown] explicitly stops [TrackingService] after every test: it is a real Android `Service`,
 * a process-wide singleton Android itself keeps alive and never re-injects once created, whereas
 * each `@HiltAndroidTest` method gets a brand-new Hilt component (confirmed empirically - see the
 * report). Left running, a Service instance created by an earlier test method keeps dispatching
 * use cases built against that *earlier* method's now-discarded `SessionRepository`, silently
 * dropping every later method's Pause/Resume/location-fix intents against a repository that no
 * longer has the session id at all (this was an actual, initially-mysterious test failure during
 * development, not a hypothetical). [Context.stopService] only requests the stop and returns
 * immediately, well before [TrackingService.onDestroy] actually runs, so [tearDown] also awaits
 * [TrackingService.awaitDestroyed] - a real synchronization point on `onDestroy` completing, not a
 * fixed sleep - before letting the next test method's own `startForegroundService` call proceed.
 *
 * [FakeRepositoryModule] is nested inside this class rather than a shared top-level
 * `@TestInstallIn` module (contrast the JVM-side `app/src/test`'s `TestRepositoryModule`, shared by
 * every Robolectric test): Hilt installs `@TestInstallIn` replacements for the *entire* androidTest
 * source set with no per-test opt-out (`@UninstallModules` only accepts `@InstallIn` modules, not
 * `@TestInstallIn` ones - confirmed empirically, see [RecoveryInstrumentedTest]'s doc), so a shared
 * one here would permanently conflict with that test's need for the real, Room-backed
 * [SessionRepository]. A module nested inside a `@HiltAndroidTest` class is scoped to that class
 * alone, which is what actually makes the two tests' differing bindings coexist in one build.
 */
@HiltAndroidTest
@UninstallModules(RepositoryModule::class)
@RunWith(AndroidJUnit4::class)
class RecordFlowInstrumentedTest {

    @Module
    @InstallIn(SingletonComponent::class)
    object FakeRepositoryModule {

        @Provides
        @Singleton
        fun provideSessionRepository(): SessionRepository = MockedSessionRepository()

        @Provides
        @Singleton
        fun provideLocationTrackingRepository(): LocationTrackingRepository = MockedLocationTrackingRepository()

        @Provides
        @Singleton
        fun provideClock(): Clock = WallClock()
    }

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS)

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var locationTrackingRepository: LocationTrackingRepository

    private val mockedSessionRepository: MockedSessionRepository
        get() = sessionRepository as MockedSessionRepository

    private val mockedLocationTrackingRepository: MockedLocationTrackingRepository
        get() = locationTrackingRepository as MockedLocationTrackingRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        // Defensive: each test method gets its own freshly-built Hilt component (and therefore a
        // brand-new MockedSessionRepository instance) in practice, but resetting explicitly here
        // costs nothing and removes any dependency on that lifecycle detail staying true.
        mockedSessionRepository.reset()
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun tearDown() {
        // TrackingService is a real Android Service - a process-wide singleton that outlives this
        // one test method, unlike the fresh Hilt component/fakes each method gets (see this
        // method's own doc). Stopping it here forces its next startForegroundService call to
        // create a brand-new instance, freshly injected against whichever test's Hilt component is
        // current, instead of silently keeping this test's now-stale SessionRepository reference.
        ApplicationProvider.getApplicationContext<Context>()
            .stopService(Intent(ApplicationProvider.getApplicationContext(), TrackingService::class.java))
        // stopService() only requests the stop and returns immediately - awaitDestroyed() blocks
        // until TrackingService.onDestroy() has genuinely finished, so the next test method's own
        // startForegroundService call can never race this still-tearing-down instance.
        assertTrue("Expected TrackingService.onDestroy() to finish before teardown returns", TrackingService.awaitDestroyed())
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    private fun activeSessionId(): String? = runBlocking { mockedSessionRepository.getActiveSessionId() }

    @Test
    fun givenPermissionGranted_whenRecordTapped_thenSessionStartsAndUiReflectsARunningSession() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.history_record_button)).perform(click())

            assertNotNull("Expected a new session to have been created", activeSessionId())
            onView(withId(R.id.record_paused_tag)).check(matches(not(isDisplayed())))

            val sessionId = requireNotNull(activeSessionId())
            EspressoIdlingResource.increment()
            mockedLocationTrackingRepository.emitFix(
                RawLocationFix(
                    sessionId = sessionId,
                    latitude = 10.7626,
                    longitude = 106.6602,
                    timestamp = 1_000L,
                    horizontalAccuracyMeters = 5f,
                    speedMetersPerSec = 5f,
                ),
            )

            onView(withId(R.id.record_current_speed_value)).check(matches(withText("18.0")))
        }
    }

    @Test
    fun givenSessionRunning_whenPauseThenResumeTapped_thenButtonsReflectCorrectStateEachTime() {
        mockedSessionRepository.seedSession(
            Session(
                id = "instrumented-session-1",
                startTimestamp = System.currentTimeMillis(),
                pausedDurationMillis = 0L,
                status = SessionStatus.RUNNING,
                stoppedTimestamp = null,
                finalDistanceMeters = null,
                finalAverageSpeedMps = null,
                thumbnailPath = null,
            ),
        )

        // A concrete active session at cold start routes MainActivity straight to RecordFragment
        // (Phase 4's recovery routing) - the same entry RecoveryInstrumentedTest exercises.
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.record_paused_tag)).check(matches(not(isDisplayed())))
            onView(withId(R.id.record_pause_resume_button))
                .check(matches(withContentDescription(R.string.record_pause_content_description)))

            onView(withId(R.id.record_pause_resume_button)).perform(click())

            onView(withId(R.id.record_paused_tag)).check(matches(isDisplayed()))
            onView(withId(R.id.record_pause_resume_button))
                .check(matches(withContentDescription(R.string.record_resume_content_description)))

            onView(withId(R.id.record_pause_resume_button)).perform(click())

            onView(withId(R.id.record_paused_tag)).check(matches(not(isDisplayed())))
            onView(withId(R.id.record_pause_resume_button))
                .check(matches(withContentDescription(R.string.record_pause_content_description)))
        }
    }
}
