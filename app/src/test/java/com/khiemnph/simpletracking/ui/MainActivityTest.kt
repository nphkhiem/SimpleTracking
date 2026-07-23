package com.khiemnph.simpletracking.ui

import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import com.khiemnph.domain.fake.MockedSessionRepository
import com.khiemnph.domain.model.Session
import com.khiemnph.domain.model.SessionStatus
import com.khiemnph.domain.repository.SessionRepository
import com.khiemnph.simpletracking.R
import com.khiemnph.simpletracking.ui.record.RecordFragment
import com.khiemnph.simpletracking.ui.record.RecordFragmentArgs
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import javax.inject.Inject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Exercises [MainActivity] through a real, Dagger-generated Hilt injection path -
 * `Hilt_MainActivity`'s `onCreate()`, not a manual field-assignment bypass - by running under
 * [HiltTestApplication] with [TestRepositoryModule][com.khiemnph.simpletracking.di.TestRepositoryModule]
 * swapping in an in-memory [MockedSessionRepository] for the real Room-backed one.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [33])
class MainActivityTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var sessionRepository: SessionRepository

    private val mockedSessionRepository: MockedSessionRepository
        get() = sessionRepository as MockedSessionRepository

    private val sessionId = "session-42"

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    private fun seedActiveSession() {
        mockedSessionRepository.seedSession(
            Session(
                id = sessionId,
                startTimestamp = 0L,
                pausedDurationMillis = 0L,
                status = SessionStatus.RUNNING,
                stoppedTimestamp = null,
                finalDistanceMeters = null,
                finalAverageSpeedMps = null,
                thumbnailPath = null,
            ),
        )
    }

    private fun navControllerOf(activity: MainActivity) =
        (activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController

    private fun idleMainLooper() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun givenActiveSessionExistsAtLaunch_whenActivityStarted_thenNavControllerNavigatesToRecordFragmentWithSessionId() {
        seedActiveSession()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            idleMainLooper()

            scenario.onActivity { activity ->
                val navController = navControllerOf(activity)
                assertEquals(R.id.recordFragment, navController.currentDestination?.id)
                val args = RecordFragmentArgs.fromBundle(navController.currentBackStackEntry!!.arguments!!)
                assertEquals(sessionId, args.sessionId)
            }
        }
    }

    @Test
    fun givenNoActiveSession_whenActivityStarted_thenHistoryFragmentRemainsCurrentDestination() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            idleMainLooper()

            scenario.onActivity { activity ->
                val navController = navControllerOf(activity)
                assertEquals(R.id.historyFragment, navController.currentDestination?.id)
            }
        }
    }

    @Test
    fun givenAlreadyOnRecordFragmentWithActiveSession_whenOnStartFiresAgain_thenNoRedundantNavigation() {
        seedActiveSession()
        var destinationChangedCount = 0
        var backStackSizeAfterFirstStart = 0
        val capturedThrowables = mutableListOf<Throwable>()

        // A wrong implementation that re-invokes navigate() while already on recordFragment
        // throws (the action is only declared from historyFragment) - but since that call is made
        // from a lifecycleScope coroutine, the exception never reaches JUnit on its own; it is
        // instead delivered to the thread's uncaught-exception handler. Capturing that handler is
        // what makes this test an effective regression guard rather than one that passes no
        // matter what MainActivity's onStart does.
        val previousUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, throwable -> capturedThrowables += throwable }

        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                idleMainLooper()
                scenario.onActivity { activity ->
                    val navController = navControllerOf(activity)
                    backStackSizeAfterFirstStart = navController.currentBackStack.value.size
                    // addOnDestinationChangedListener immediately replays the current destination
                    // once on registration; only count changes that happen after that.
                    navController.addOnDestinationChangedListener { _, _, _ -> destinationChangedCount++ }
                    destinationChangedCount = 0
                }

                // Simulates the user pressing Home then reopening from Recents: onStop/onStart
                // fire again on this same, already-alive Activity instance rather than a fresh
                // onCreate.
                scenario.moveToState(Lifecycle.State.CREATED)
                scenario.moveToState(Lifecycle.State.RESUMED)
                idleMainLooper()

                assertTrue("Expected no navigation attempt, but caught: $capturedThrowables", capturedThrowables.isEmpty())
                assertEquals(0, destinationChangedCount)
                scenario.onActivity { activity ->
                    val navController = navControllerOf(activity)
                    assertEquals(R.id.recordFragment, navController.currentDestination?.id)
                    assertEquals(backStackSizeAfterFirstStart, navController.currentBackStack.value.size)
                }
            }
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previousUncaughtExceptionHandler)
        }
    }

    @Test
    fun givenObserveActiveSessionUseCaseThrows_whenActivityStarted_thenActivityDoesNotCrashAndNoNavigationOccurs() {
        mockedSessionRepository.throwOnObserveActiveSession(RuntimeException("Simulated Room query failure"))
        val capturedThrowables = mutableListOf<Throwable>()
        val previousUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, throwable -> capturedThrowables += throwable }

        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                idleMainLooper()

                assertTrue(
                    "Expected the active-session check failure to be caught, but it crashed with: $capturedThrowables",
                    capturedThrowables.isEmpty(),
                )
                scenario.onActivity { activity ->
                    val navController = navControllerOf(activity)
                    assertEquals(R.id.historyFragment, navController.currentDestination?.id)
                }
            }
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previousUncaughtExceptionHandler)
        }
    }

    @Test
    fun givenActiveSessionExistsAtLaunch_whenRecordFragmentCreated_thenSessionIdArgumentArrivesViaNavArgs() {
        seedActiveSession()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            idleMainLooper()

            scenario.onActivity { activity ->
                val navHostFragment =
                    activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val recordFragment =
                    navHostFragment.childFragmentManager.primaryNavigationFragment as RecordFragment
                assertEquals(sessionId, recordFragment.args.sessionId)
            }
        }
    }
}
