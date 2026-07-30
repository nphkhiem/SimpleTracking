package com.khiemnph.simpletracking.ui

import android.os.Looper
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import com.khiemnph.simpletracking.R
import com.khiemnph.simpletracking.ui.detail.SessionDetailFragmentArgs
import com.khiemnph.simpletracking.ui.summary.SummaryFragmentArgs
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Proves the three new destinations can actually be reached and instantiated.
 *
 * They are placeholders, which is exactly why this is worth testing now. A missing
 * `@AndroidEntryPoint`, a bad argument type or a broken `ComposeView` setup would not surface until
 * the phase that builds the real screen navigated to it for the first time, which is the worst
 * moment to discover the shell was never wired up correctly.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [33])
class PlaceholderDestinationsTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    private fun navigateAndAssertDestination(destinationId: Int, navigate: (NavHostFragment) -> Unit) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            shadowOf(Looper.getMainLooper()).idle()
            scenario.onActivity { activity ->
                val navHost =
                    activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                navigate(navHost)
            }
            shadowOf(Looper.getMainLooper()).idle()
            scenario.onActivity { activity ->
                val navHost =
                    activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                assertEquals(destinationId, navHost.navController.currentDestination?.id)
            }
        }
    }

    @Test
    fun `summary can be reached with a session id`() {
        navigateAndAssertDestination(R.id.summaryFragment) { navHost ->
            navHost.navController.navigate(
                R.id.summaryFragment,
                SummaryFragmentArgs(sessionId = "session-1").toBundle(),
            )
        }
    }

    @Test
    fun `session detail can be reached with a session id`() {
        navigateAndAssertDestination(R.id.sessionDetailFragment) { navHost ->
            navHost.navController.navigate(
                R.id.sessionDetailFragment,
                SessionDetailFragmentArgs(sessionId = "session-1").toBundle(),
            )
        }
    }

    @Test
    fun `settings can be reached`() {
        navigateAndAssertDestination(R.id.settingsFragment) { navHost ->
            navHost.navController.navigate(R.id.settingsFragment)
        }
    }
}
