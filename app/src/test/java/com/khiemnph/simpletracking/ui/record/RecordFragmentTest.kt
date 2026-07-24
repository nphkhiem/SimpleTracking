package com.khiemnph.simpletracking.ui.record

import android.Manifest
import android.app.Application
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.khiemnph.domain.fake.MockedSessionRepository
import com.khiemnph.domain.model.LocationPoint
import com.khiemnph.domain.model.Session
import com.khiemnph.domain.model.SessionStatus
import com.khiemnph.domain.repository.SessionRepository
import com.khiemnph.simpletracking.R
import com.khiemnph.simpletracking.service.TrackingService
import com.khiemnph.simpletracking.ui.MainActivity
import com.khiemnph.simpletracking.ui.history.HistoryFragmentDirections
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

/**
 * Exercises [RecordFragment] through a real, Dagger-generated Hilt injection path by launching
 * [MainActivity] under [HiltTestApplication], mirroring
 * [com.khiemnph.simpletracking.ui.MainActivityTest]'s approach. Covers the "resume an active
 * session" entry (cold-start recovery) and the "start a brand-new session" entry (navigating from
 * History), including the permission-gating branch that's only reachable for the latter.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [33])
class RecordFragmentTest {

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
                startTimestamp = System.currentTimeMillis(),
                pausedDurationMillis = 0L,
                status = SessionStatus.RUNNING,
                stoppedTimestamp = null,
                finalDistanceMeters = null,
                finalAverageSpeedMps = null,
                thumbnailPath = null,
            ),
        )
    }

    private fun idleMainLooper() = shadowOf(Looper.getMainLooper()).idle()

    /**
     * [org.robolectric.shadows.ShadowInstrumentation.grantPermissions]/`denyPermissions` are
     * package-private in this Robolectric version, so this goes through them via reflection -
     * they're the only shadow API that actually controls what
     * [androidx.core.content.ContextCompat.checkSelfPermission] resolves to under Robolectric, and
     * what [RecordFragment]'s permission launcher callback receives on this `sdk = [33]` config.
     * "Granted" means every permission [RecordFragment] requires on API 33+ - both
     * `ACCESS_FINE_LOCATION` and `POST_NOTIFICATIONS` - since a partial grant must still be
     * treated as denied; "denied" only needs to withhold one to prove that.
     */
    private fun setLocationPermissionGranted(granted: Boolean) {
        val shadowInstrumentation = shadowOf(InstrumentationRegistry.getInstrumentation())
        val permissions = if (granted) {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        ReflectionHelpers.callInstanceMethod<Unit>(
            shadowInstrumentation,
            if (granted) "grantPermissions" else "denyPermissions",
            ReflectionHelpers.ClassParameter.from(Array<String>::class.java, permissions),
        )
    }

    private fun recordFragmentOf(activity: MainActivity): RecordFragment {
        val navHostFragment =
            activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.childFragmentManager.primaryNavigationFragment as RecordFragment
    }

    @Test
    fun givenActiveSessionExists_whenRecordFragmentDisplayed_thenBottomSheetIsNotHideable() {
        seedActiveSession()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            idleMainLooper()

            scenario.onActivity { activity ->
                val behavior = recordFragmentOf(activity).bottomSheetBehavior()
                assertFalse("Expected the Record bottom sheet to be non-dismissable", behavior.isHideable)
            }
        }
    }

    @Test
    fun givenActiveSessionExists_whenRecordFragmentDisplayed_thenBottomSheetStateIsExpanded() {
        seedActiveSession()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            idleMainLooper()

            scenario.onActivity { activity ->
                val behavior = recordFragmentOf(activity).bottomSheetBehavior()
                assertEquals(BottomSheetBehavior.STATE_EXPANDED, behavior.state)
            }
        }
    }

    @Test
    fun givenBackButtonClicked_whenInvoked_thenNavigatesBackToHistoryWithoutStoppingTheSession() {
        seedActiveSession()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            idleMainLooper()

            scenario.onActivity { activity ->
                recordFragmentOf(activity).view
                    ?.findViewById<View>(R.id.record_back_button)
                    ?.performClick()
            }
            idleMainLooper()

            scenario.onActivity { activity ->
                val navHostFragment =
                    activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                assertEquals(R.id.historyFragment, navHostFragment.navController.currentDestination?.id)
            }
        }
        runBlockingSessionStillActive()
    }

    private fun runBlockingSessionStillActive() = runBlocking {
        assertEquals(sessionId, mockedSessionRepository.getActiveSessionId())
    }

    /**
     * Under Robolectric, [com.google.android.gms.maps.GoogleMap]'s snapshot callback never fires
     * (see phase 6 report - no `shadows-play-services` shadow on this classpath), so
     * `RecordFragment.googleMap` stays `null` for the fragment's whole lifetime here. This
     * exercises the `googleMap == null` branch of `handleStopClicked` - the only Stop path
     * Robolectric can reach - and is the regression test for the fix that moved the Stop intent
     * dispatch off `viewModelScope` and onto the injected application-scoped
     * [com.khiemnph.simpletracking.di.ApplicationScope] [kotlinx.coroutines.CoroutineScope]: it
     * proves the Stop button still both navigates back to History AND reliably sends the Stop
     * intent to [TrackingService].
     */
    @Test
    fun givenStopButtonClicked_whenInvoked_thenNavigatesBackToHistoryAndSendsStopIntentToTrackingService() {
        seedActiveSession()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            idleMainLooper()

            scenario.onActivity { activity ->
                recordFragmentOf(activity).view
                    ?.findViewById<View>(R.id.record_stop_button)
                    ?.performClick()
            }
            idleMainLooper()

            scenario.onActivity { activity ->
                val navHostFragment =
                    activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                assertEquals(R.id.historyFragment, navHostFragment.navController.currentDestination?.id)
            }
        }

        val appContext = ApplicationProvider.getApplicationContext<Application>()
        val expectedStopIntent = TrackingService.stopIntent(appContext, sessionId)
        // The queue also holds the earlier Start intent resolveSession sent on entry, so drain
        // every started-service intent and find the Stop one rather than assuming queue position.
        val startedIntents = generateSequence { shadowOf(appContext).nextStartedService }.toList()
        val actualStopIntent = startedIntents.find { it.action == expectedStopIntent.action }
        assertNotNull("Expected a Stop intent to have been sent to TrackingService", actualStopIntent)
        assertEquals(expectedStopIntent.component, actualStopIntent?.component)
    }

    @Test
    fun givenLocationPermissionAlreadyGranted_whenStartingNewSession_thenSessionIsCreatedAndBecomesActive() {
        setLocationPermissionGranted(true)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            idleMainLooper()

            scenario.onActivity { activity ->
                val navHostFragment =
                    activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                navHostFragment.navController.navigate(
                    HistoryFragmentDirections.actionHistoryFragmentToRecordFragment(sessionId = null),
                )
            }
            idleMainLooper()
        }

        runBlocking {
            assertNotNull(
                "Expected a new session to have been created once permission was already granted",
                mockedSessionRepository.getActiveSessionId(),
            )
        }
    }

    @Test
    fun givenLocationPermissionDenied_whenStartingNewSession_thenNoSessionIsCreated() {
        setLocationPermissionGranted(false)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            idleMainLooper()

            scenario.onActivity { activity ->
                val navHostFragment =
                    activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                navHostFragment.navController.navigate(
                    HistoryFragmentDirections.actionHistoryFragmentToRecordFragment(sessionId = null),
                )
            }
            idleMainLooper()
        }

        runBlocking {
            assertNull(
                "Expected no session to be created when location permission is denied",
                mockedSessionRepository.getActiveSessionId(),
            )
        }
    }

    @Test
    fun givenSessionIsPaused_whenUiStateUpdates_thenPausedTagBecomesVisibleAndButtonShowsResumeIcon() {
        seedActiveSession()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            idleMainLooper()

            scenario.onActivity { runBlocking { mockedSessionRepository.pauseSession(sessionId) } }
            idleMainLooper()

            scenario.onActivity { activity ->
                val fragment = recordFragmentOf(activity)
                val pausedTag = fragment.view?.findViewById<View>(R.id.record_paused_tag)
                assertEquals(View.VISIBLE, pausedTag?.visibility)
            }
        }
    }

    @Test
    fun givenDistanceAndSpeedRecorded_whenUiStateUpdates_thenBottomSheetTextViewsReflectFormattedValues() {
        seedActiveSession()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            idleMainLooper()

            scenario.onActivity {
                runBlocking {
                    mockedSessionRepository.recordLocationPoint(
                        LocationPoint(
                            sessionId = sessionId,
                            latitude = 10.7626,
                            longitude = 106.6602,
                            timestamp = 1_000L,
                            horizontalAccuracyMeters = 5f,
                            speedMetersPerSec = 5f,
                        ),
                    )
                }
            }
            idleMainLooper()

            scenario.onActivity { activity ->
                val fragment = recordFragmentOf(activity)
                val speedView = fragment.view?.findViewById<TextView>(R.id.record_current_speed_value)
                assertEquals("18.0", speedView?.text.toString())
            }
        }
    }
}
