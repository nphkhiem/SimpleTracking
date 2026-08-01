package com.khiemnph.simpletracking.ui.route

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.khiemnph.domain.model.LatLngPoint
import com.khiemnph.simpletracking.R
import com.khiemnph.simpletracking.ui.theme.ChayNgayDiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The hero has three outcomes, and two of them are "nothing to draw" for different reasons. They
 * are worth telling apart: one says the recording never got going, the other says the recording
 * worked and the run did not move.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RouteHeroTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun render(points: List<LatLngPoint>) {
        composeRule.setContent { ChayNgayDiTheme { RouteHero(points = points) } }
    }

    @Test
    fun `a route with a shape is drawn`() {
        render(
            listOf(
                LatLngPoint(21.0278, 105.8342),
                LatLngPoint(21.0288, 105.8352),
                LatLngPoint(21.0298, 105.8362),
            ),
        )

        composeRule.onNodeWithTag(RouteHeroTestTags.TRACE).assertIsDisplayed()
    }

    @Test
    fun `a single point says there is not enough map data`() {
        render(listOf(LatLngPoint(21.0278, 105.8342)))

        composeRule.onNodeWithTag(RouteHeroTestTags.SPARSE)
            .assertTextEquals(context.getString(R.string.route_not_enough_points))
    }

    @Test
    fun `an empty route says there is not enough map data`() {
        render(emptyList())

        composeRule.onNodeWithTag(RouteHeroTestTags.SPARSE)
            .assertTextEquals(context.getString(R.string.route_not_enough_points))
    }

    @Test
    fun `points that never move say so, rather than drawing a lone dot`() {
        // Standing still with the GPS running: plenty of fixes, no extent. This drew as a single
        // dot in a large empty frame, which reads as a failed render rather than as a fact.
        render(List(30) { LatLngPoint(21.0278, 105.8342) })

        composeRule.onNodeWithTag(RouteHeroTestTags.SPARSE)
            .assertTextEquals(context.getString(R.string.route_no_movement))
    }

    @Test
    fun `movement on one axis only is still a shape`() {
        // A straight north-south run has no longitude extent, and must not be mistaken for
        // standing still.
        render(
            listOf(
                LatLngPoint(21.0278, 105.8342),
                LatLngPoint(21.0288, 105.8342),
                LatLngPoint(21.0298, 105.8342),
            ),
        )

        composeRule.onNodeWithTag(RouteHeroTestTags.TRACE).assertIsDisplayed()
    }
}
