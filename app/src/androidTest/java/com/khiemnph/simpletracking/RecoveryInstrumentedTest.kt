package com.khiemnph.simpletracking

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.khiemnph.data.di.RepositoryModule
import com.khiemnph.data.repository.SessionRepositoryImpl
import com.khiemnph.data.util.Clock
import com.khiemnph.data.util.WallClock
import com.khiemnph.domain.fake.MockedLocationTrackingRepository
import com.khiemnph.domain.repository.LocationTrackingRepository
import com.khiemnph.domain.repository.SessionRepository
import com.khiemnph.simpletracking.service.TrackingService
import com.khiemnph.simpletracking.ui.MainActivity
import com.khiemnph.simpletracking.ui.record.RecordFragment
import com.khiemnph.simpletracking.util.EspressoIdlingResource
import dagger.Binds
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
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves [MainActivity]'s cold-start recovery routing (Phase 4) survives a real restart against
 * the real, Room-backed [SessionRepository] - not [com.khiemnph.domain.fake.MockedSessionRepository]'s
 * in-memory state, which [com.khiemnph.simpletracking.ui.MainActivityTest]'s Robolectric suite
 * already exercises.
 *
 * Judgment call #1 - fakes vs. real Room: [RealSessionRepositoryModule] is nested inside this
 * class, not a shared module, and this test alone uninstalls the production [RepositoryModule] to
 * make room for it. [RecordFlowInstrumentedTest]'s own `FakeRepositoryModule` does the same in
 * reverse. Both classes need their own `@UninstallModules(RepositoryModule::class)` because Hilt's
 * `@TestInstallIn` (the JVM suite's pattern for this, see `app/src/test`'s `TestRepositoryModule`)
 * installs its replacement for the *entire* androidTest source set with no per-test opt-out -
 * confirmed empirically: `@UninstallModules` rejects `@TestInstallIn` modules outright ("should
 * only include modules annotated with both @Module and @InstallIn"), so a shared `@TestInstallIn`
 * fake here would permanently conflict with this test's need for the real repository. A plain
 * `@Module` nested inside a `@HiltAndroidTest` class, by contrast, is scoped to that one class.
 *
 * Judgment call #2 - no real process kill: the brief asks for this via an actual OS-level process
 * kill (`am force-stop`/`UiDevice`). An instrumented test's own code runs inside the very process
 * being instrumented, so force-stopping the app under test kills the test JVM that would go on to
 * assert anything afterward - confirmed empirically against this project's emulator, where a
 * self-inflicted `am force-stop` reports "Instrumentation run failed due to Process crashed" and
 * aborts the whole `connectedAndroidTest` invocation, not just this one test (see report). Since
 * [RepositoryModule]'s database is a real file (`simple_tracking.db`, unaffected by `am force-stop`,
 * only by `pm clear`), the same persisted-recovery behavior is instead proven by closing the first
 * [ActivityScenario] entirely (destroying that Activity, not just backgrounding it) and launching a
 * brand-new one - a fresh `MainActivity` instance reading only real, on-disk state, with no
 * in-memory object surviving from before. This is the practical ceiling of what
 * `connectedAndroidTest` can verify without Android Test Orchestrator's per-test process isolation,
 * which this phase does not set up (see report).
 */
@HiltAndroidTest
@UninstallModules(RepositoryModule::class)
@RunWith(AndroidJUnit4::class)
class RecoveryInstrumentedTest {

    @Module
    @InstallIn(SingletonComponent::class)
    abstract class RealSessionRepositoryModule {

        @Binds
        @Singleton
        abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

        companion object {
            @Provides
            @Singleton
            fun provideLocationTrackingRepository(): LocationTrackingRepository = MockedLocationTrackingRepository()

            @Provides
            @Singleton
            fun provideClock(): Clock = WallClock()
        }
    }

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS)

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Before
    fun setUp() {
        // The real Room DB is a file on disk that outlives this one test's process and even this
        // whole instrumentation invocation - start every run from a pristine database rather than
        // risking a leftover active session from a previous run tripping the LIMIT-1,
        // no-ORDER-BY query in SessionDao.observeActiveSession/getActiveSessionId.
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(DATABASE_FILE_NAME)
        hiltRule.inject()
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun tearDown() {
        // See RecordFlowInstrumentedTest.tearDown()'s doc: TrackingService outlives this one test
        // method as a real Android Service, so it must be stopped explicitly or a later test's
        // Service instance would silently keep this test's now-discarded SessionRepository.
        ApplicationProvider.getApplicationContext<Context>()
            .stopService(Intent(ApplicationProvider.getApplicationContext(), TrackingService::class.java))
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    @Test
    fun givenAppProcessKilledWhileSessionRunning_whenRelaunched_thenNavigatesStraightToRecordFragmentWithCorrectRunningState() {
        val sessionId = ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.history_record_button)).perform(click())
            onView(withId(R.id.record_paused_tag)).check(matches(not(isDisplayed())))

            runBlocking { requireNotNull(sessionRepository.getActiveSessionId()) }
        }
        // ActivityScenario.close() (implicit via the `use` above) tears the Activity all the way
        // down - nothing from the closed scenario is reused below, only the real Room row it
        // persisted, exactly mirroring what a relaunch after a real process kill would read.

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // ActivityScenario.onActivity {} is a raw runOnMainSync callback, not an Espresso
            // ViewInteraction - it never consults IdlingRegistry, so it can run before MainActivity's
            // onStart recovery coroutine (correctly instrumented via EspressoIdlingResource) has
            // finished. Espresso.onIdle() runs the exact same IdlingRegistry synchronization
            // ViewInteraction.perform()/check() use internally, but without matching any view, so
            // it can't race the outgoing/incoming Activity windows during this relaunch the way an
            // onView(withId(...)) check briefly could (confirmed empirically: that approach hit an
            // AmbiguousViewMatcherException from a transient second nav_host_fragment window during
            // the Activity transition). This guarantees the recovery coroutine has fully resolved
            // before the onActivity assertion below ever runs.
            onIdle()
            scenario.onActivity { activity ->
                val navHostFragment =
                    activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                assertEquals(
                    "Expected cold-start recovery to route straight to RecordFragment",
                    R.id.recordFragment,
                    navHostFragment.navController.currentDestination?.id,
                )
                val recordFragment = navHostFragment.childFragmentManager.primaryNavigationFragment as RecordFragment
                assertEquals(
                    "Expected the recovered session id to match the one persisted before relaunch",
                    sessionId,
                    recordFragment.args.sessionId,
                )
            }
            onView(withId(R.id.record_paused_tag)).check(matches(not(isDisplayed())))
        }
    }

    private companion object {
        private const val DATABASE_FILE_NAME = "simple_tracking.db"
    }
}
