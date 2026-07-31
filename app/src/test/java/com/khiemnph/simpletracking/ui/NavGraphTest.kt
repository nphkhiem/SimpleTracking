package com.khiemnph.simpletracking.ui

import android.content.Context
import androidx.navigation.NavDestination
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.khiemnph.simpletracking.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Locks down the navigation graph's shape.
 *
 * Three destinations here are still placeholder screens. Landing the whole graph in one change is
 * what stops the phases that build them from editing this file in parallel, but it also means the
 * graph is currently the only description of how those screens connect. These assertions are that
 * description, so a later change cannot quietly drop `launchSingleTop` or the `popUpTo` that keeps
 * Stop from emptying the back stack.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NavGraphTest {

    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        navController = TestNavHostController(context)
        navController.setGraph(R.navigation.nav_graph)
    }

    private fun destination(id: Int): NavDestination =
        requireNotNull(navController.graph.findNode(id)) { "destination missing from nav_graph" }

    @Test
    fun `runs is the start destination`() {
        assertEquals(R.id.runsFragment, navController.graph.startDestinationId)
    }

    @Test
    fun `every destination the revamp needs exists`() {
        listOf(
            R.id.runsFragment,
            R.id.recordFragment,
            R.id.summaryFragment,
            R.id.sessionDetailFragment,
            R.id.settingsFragment,
        ).forEach { assertNotNull(navController.graph.findNode(it)) }
    }

    @Test
    fun `navigating to record is single top so recovery cannot stack two record screens`() {
        val action = destination(R.id.runsFragment).getAction(R.id.action_runsFragment_to_recordFragment)

        assertNotNull("Runs must be able to reach Record", action)
        assertTrue(
            "Two entry points lead to Record; without launchSingleTop they can stack it twice",
            action!!.navOptions?.shouldLaunchSingleTop() == true,
        )
    }

    @Test
    fun `stop pops record on the way to summary, without ever emptying the stack`() {
        val action = destination(R.id.recordFragment).getAction(R.id.action_recordFragment_to_summaryFragment)

        assertNotNull("Stop needs a destination", action)
        assertEquals(R.id.summaryFragment, action!!.destinationId)
        assertEquals(
            "popUpTo must target Runs, so Back from Summary lands there rather than on a finished run",
            R.id.runsFragment,
            action.navOptions?.popUpToId,
        )
        assertTrue(
            "popUpTo must not be inclusive, or this pops the stack empty and drops out to the launcher",
            action.navOptions?.isPopUpToInclusive() == false,
        )
    }

    @Test
    fun `runs can reach session detail and settings`() {
        val runs = destination(R.id.runsFragment)

        assertEquals(
            R.id.sessionDetailFragment,
            runs.getAction(R.id.action_runsFragment_to_sessionDetailFragment)?.destinationId,
        )
        assertEquals(
            R.id.settingsFragment,
            runs.getAction(R.id.action_runsFragment_to_settingsFragment)?.destinationId,
        )
    }

    @Test
    fun `summary can reach session detail`() {
        assertEquals(
            R.id.sessionDetailFragment,
            destination(R.id.summaryFragment)
                .getAction(R.id.action_summaryFragment_to_sessionDetailFragment)?.destinationId,
        )
    }

    /**
     * Every action animates. The app had no motion at all, and a destination added later without
     * transitions would appear instantly next to ones that do not, which reads as a bug rather
     * than a choice.
     */
    @Test
    fun `every action declares enter and exit transitions`() {
        val missing = mutableListOf<String>()

        listOf(
            R.id.runsFragment to listOf(
                R.id.action_runsFragment_to_recordFragment,
                R.id.action_runsFragment_to_sessionDetailFragment,
                R.id.action_runsFragment_to_settingsFragment,
            ),
            R.id.recordFragment to listOf(R.id.action_recordFragment_to_summaryFragment),
            R.id.summaryFragment to listOf(R.id.action_summaryFragment_to_sessionDetailFragment),
        ).forEach { (destinationId, actionIds) ->
            actionIds.forEach { actionId ->
                val options = destination(destinationId).getAction(actionId)?.navOptions
                if (options?.enterAnim == -1 || options?.exitAnim == -1) missing += "action $actionId"
                if (options?.popEnterAnim == -1 || options?.popExitAnim == -1) missing += "pop of action $actionId"
            }
        }

        assertEquals(emptyList<String>(), missing)
    }
}
