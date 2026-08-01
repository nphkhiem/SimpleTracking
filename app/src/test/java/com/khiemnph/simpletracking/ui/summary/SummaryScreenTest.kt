package com.khiemnph.simpletracking.ui.summary

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.khiemnph.domain.model.LatLngPoint
import com.khiemnph.simpletracking.R
import com.khiemnph.simpletracking.testing.DefaultLocaleRule
import com.khiemnph.simpletracking.ui.route.RouteHeroTestTags
import com.khiemnph.simpletracking.ui.theme.ChayNgayDiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SummaryScreenTest {

    @get:Rule
    val localeRule = DefaultLocaleRule()

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val route = listOf(
        LatLngPoint(21.0278, 105.8342),
        LatLngPoint(21.0288, 105.8342),
        LatLngPoint(21.0288, 105.8352),
    )

    private fun ready(
        distanceKm: String = "5.42",
        isTooShortToKeep: Boolean = false,
        routePoints: List<LatLngPoint> = route,
    ) = SummaryUiState.Ready(
        distanceKm = distanceKm,
        durationLabel = "28:14",
        paceLabel = "5:12",
        routePoints = routePoints,
        isTooShortToKeep = isTooShortToKeep,
    )

    private fun render(
        state: SummaryUiState,
        onKeep: () -> Unit = {},
        onDiscard: () -> Unit = {},
    ) {
        composeRule.setContent {
            ChayNgayDiTheme { SummaryScreen(state = state, onKeep = onKeep, onDiscard = onDiscard) }
        }
    }

    @Test
    fun `shows the route and both actions for a finished run`() {
        render(ready())

        composeRule.onNodeWithTag(SummaryTestTags.HEADLINE).assertIsDisplayed()
        composeRule.onNodeWithTag(RouteHeroTestTags.TRACE).assertIsDisplayed()
        composeRule.onNodeWithTag(SummaryTestTags.KEEP).assertIsDisplayed()
        composeRule.onNodeWithTag(SummaryTestTags.DISCARD).assertIsDisplayed()
    }

    @Test
    fun `keeping the run reports it once`() {
        var kept = 0
        render(ready(), onKeep = { kept++ })

        composeRule.onNodeWithTag(SummaryTestTags.KEEP).performClick()

        assertEquals(1, kept)
    }

    @Test
    fun `discarding the run reports it once`() {
        var discarded = 0
        render(ready(), onDiscard = { discarded++ })

        composeRule.onNodeWithTag(SummaryTestTags.DISCARD).performClick()

        assertEquals(1, discarded)
    }

    @Test
    fun `a normal run is not offered for deletion`() {
        render(ready(isTooShortToKeep = false))

        composeRule.onNodeWithTag(SummaryTestTags.TOO_SHORT).assertDoesNotExist()
    }

    @Test
    fun `a run too short to be real offers to delete itself`() {
        render(ready(distanceKm = "0.00", isTooShortToKeep = true))

        composeRule.onNodeWithTag(SummaryTestTags.TOO_SHORT).assertIsDisplayed()
    }

    @Test
    fun `the headline congratulates a real run`() {
        render(ready(isTooShortToKeep = false))

        composeRule.onNodeWithTag(SummaryTestTags.HEADLINE)
            .assertTextEquals(context.getString(R.string.summary_headline))
    }

    @Test
    fun `the headline does not congratulate a run it is offering to delete`() {
        // The screen used to say "Nice run" directly above "That was a very short run. Delete it?",
        // praising and dismissing the same thirty seconds in one screenful.
        render(ready(distanceKm = "0.00", isTooShortToKeep = true))

        composeRule.onNodeWithTag(SummaryTestTags.HEADLINE)
            .assertTextEquals(context.getString(R.string.summary_headline_too_short))
    }

    @Test
    fun `a run with no usable route still shows its numbers`() {
        // Stopped within a second of starting, or never got a fix. There is no shape to draw, but
        // the metrics are real and the screen must not be an empty box.
        render(ready(routePoints = emptyList()))

        composeRule.onNodeWithTag(RouteHeroTestTags.SPARSE).assertIsDisplayed()
        composeRule.onNodeWithTag(SummaryTestTags.KEEP).assertIsDisplayed()
    }

    @Test
    fun `a session that cannot be read says so and offers a way back`() {
        var back = 0
        render(SummaryUiState.NotFound, onKeep = { back++ })

        composeRule.onNodeWithTag(SummaryTestTags.NOT_FOUND).assertIsDisplayed()
        composeRule.onNodeWithTag(SummaryTestTags.KEEP).assertDoesNotExist()
    }
}
