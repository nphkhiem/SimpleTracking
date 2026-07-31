package com.khiemnph.simpletracking.ui.record

import android.os.Looper
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import com.khiemnph.domain.fake.MockedSessionRepository
import com.khiemnph.domain.model.Session
import com.khiemnph.domain.model.SessionStatus
import com.khiemnph.domain.repository.SessionRepository
import com.khiemnph.simpletracking.R
import com.khiemnph.simpletracking.ui.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import javax.inject.Inject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Record must survive rotation.
 *
 * The landscape variant makes the sheet a side rail, which deliberately has no
 * `BottomSheetBehavior`. Code that assumes one crashes the moment the device turns, which is the
 * regression this exists to stop: it shipped once and only a device caught it.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [33], qualifiers = "land")
class RecordLandscapeTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        (sessionRepository as MockedSessionRepository).seedSession(
            Session(
                id = "session-1",
                startTimestamp = 0L,
                pausedDurationMillis = 0L,
                status = SessionStatus.RUNNING,
                stoppedTimestamp = null,
                finalDistanceMeters = null,
                finalAverageSpeedMps = null,
                routePolyline = null,
            ),
        )
    }

    @Test
    fun `the record screen opens in landscape without crashing`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            shadowOf(Looper.getMainLooper()).idle()

            scenario.onActivity { activity ->
                val navHost =
                    activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                assertEquals(R.id.recordFragment, navHost.navController.currentDestination?.id)
            }
        }
    }
}
