package com.khiemnph.simpletracking.ui.route

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.khiemnph.domain.model.LatLngPoint
import com.khiemnph.simpletracking.ui.theme.ChayNgayDiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers what the Record screen falls back to when the map cannot render, which offline is the
 * only thing the user sees.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OfflineRouteCanvasTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val route = listOf(
        LatLngPoint(21.0278, 105.8342),
        LatLngPoint(21.0288, 105.8342),
        LatLngPoint(21.0288, 105.8352),
    )

    private fun render(points: List<LatLngPoint>) {
        composeRule.setContent {
            ChayNgayDiTheme {
                OfflineRouteCanvas(points = points, modifier = Modifier.fillMaxSize())
            }
        }
    }

    @Test
    fun `draws the route when there are points to draw`() {
        render(route)

        composeRule.onNodeWithTag(OfflineRouteTestTags.TRACE).assertIsDisplayed()
    }

    @Test
    fun `says the map is unavailable so a bare route is not mistaken for a broken screen`() {
        render(route)

        composeRule.onNodeWithText("Map unavailable offline").assertIsDisplayed()
    }

    @Test
    fun `states the scale because the view refits itself as the route grows`() {
        render(route)

        composeRule.onNodeWithTag(OfflineRouteTestTags.SCALE_BAR).assertIsDisplayed()
    }

    @Test
    fun `draws nothing but the caption before a second point arrives`() {
        render(listOf(LatLngPoint(21.0278, 105.8342)))

        composeRule.onNodeWithTag(OfflineRouteTestTags.TRACE).assertDoesNotExist()
        composeRule.onNodeWithText("Map unavailable offline").assertIsDisplayed()
    }

    @Test
    fun `omits the scale bar when the run has not moved and there is no extent to measure`() {
        val stationary = LatLngPoint(21.0278, 105.8342)
        render(listOf(stationary, stationary, stationary))

        composeRule.onNodeWithTag(OfflineRouteTestTags.SCALE_BAR).assertDoesNotExist()
    }
}
