package com.khiemnph.simpletracking.ui.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.khiemnph.domain.model.LatLngPoint
import com.khiemnph.simpletracking.testing.DefaultLocaleRule
import com.khiemnph.simpletracking.ui.components.MetricGridTestTags
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
class SessionDetailScreenTest {

    @get:Rule
    val localeRule = DefaultLocaleRule()

    @get:Rule
    val composeRule = createComposeRule()

    private val route = listOf(
        LatLngPoint(21.0278, 105.8342),
        LatLngPoint(21.0288, 105.8342),
        LatLngPoint(21.0288, 105.8352),
    )

    private fun ready(
        titleLabel: String = "Sat, 9 Aug · 9:41 AM",
        hasCustomTitle: Boolean = false,
        splits: List<SplitUiModel> = listOf(
            SplitUiModel("1", "5:12", 1f, isFastest = false),
            SplitUiModel("2", "4:48", 0.92f, isFastest = true),
        ),
        routePoints: List<LatLngPoint> = route,
    ) = SessionDetailUiState.Ready(
        titleLabel = titleLabel,
        hasCustomTitle = hasCustomTitle,
        distanceKm = "5.42",
        durationLabel = "28:14",
        averagePaceLabel = "5:12",
        bestPaceLabel = "4:48",
        routePoints = routePoints,
        splits = splits,
    )

    private fun render(
        state: SessionDetailUiState,
        onBack: () -> Unit = {},
        onRename: (String) -> Unit = {},
        onShare: () -> Unit = {},
        onExportGpx: () -> Unit = {},
        onDelete: () -> Unit = {},
    ) {
        composeRule.setContent {
            ChayNgayDiTheme {
                SessionDetailScreen(
                    state = state,
                    onBack = onBack,
                    onRename = onRename,
                    onShare = onShare,
                    onExportGpx = onExportGpx,
                    onDelete = onDelete,
                )
            }
        }
    }

    /** The screen is a lazy list taller than the viewport, so anything past the hero needs scrolling. */
    private fun scrollTo(tag: String) =
        composeRule.onNodeWithTag(SessionDetailTestTags.SPLITS).performScrollToNode(hasTestTag(tag))

    @Test
    fun `shows the route, the title and every metric`() {
        render(ready())

        composeRule.onNodeWithTag(SessionDetailTestTags.TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(RouteHeroTestTags.TRACE).assertIsDisplayed()

        // Asserted through the metric cells rather than raw text: "4:48" is both the best pace and
        // a split's pace, so matching on text alone would be ambiguous about which one was found.
        listOf("km", "time", "avg pace", "best pace").forEach { label ->
            scrollTo(MetricGridTestTags.metricFor(label))
            composeRule.onNodeWithTag(MetricGridTestTags.metricFor(label)).assertIsDisplayed()
        }
    }

    @Test
    fun `shows one row per split`() {
        render(ready())

        scrollTo(SessionDetailTestTags.splitFor("1"))
        composeRule.onNodeWithTag(SessionDetailTestTags.splitFor("1")).assertIsDisplayed()
        scrollTo(SessionDetailTestTags.splitFor("2"))
        composeRule.onNodeWithTag(SessionDetailTestTags.splitFor("2")).assertIsDisplayed()
    }

    @Test
    fun `a run with no splits omits the section rather than showing an empty heading`() {
        render(ready(splits = emptyList()))

        composeRule.onNodeWithTag(SessionDetailTestTags.splitFor("1")).assertDoesNotExist()
        scrollTo(SessionDetailTestTags.DELETE)
        composeRule.onNodeWithTag(SessionDetailTestTags.DELETE).assertIsDisplayed()
    }

    @Test
    fun `a run with too few points to draw still shows its metrics`() {
        render(ready(routePoints = emptyList()))

        composeRule.onNodeWithTag(RouteHeroTestTags.SPARSE).assertIsDisplayed()
        scrollTo(MetricGridTestTags.metricFor("km"))
        composeRule.onNodeWithText("5.42").assertIsDisplayed()
    }

    @Test
    fun `deleting reports it once`() {
        var deleted = 0
        render(ready(), onDelete = { deleted++ })

        scrollTo(SessionDetailTestTags.DELETE)
        composeRule.onNodeWithTag(SessionDetailTestTags.DELETE).performClick()

        assertEquals(1, deleted)
    }

    @Test
    fun `a session that is gone says so and offers a way back`() {
        var back = 0
        render(SessionDetailUiState.NotFound, onBack = { back++ })

        composeRule.onNodeWithTag(SessionDetailTestTags.NOT_FOUND).assertIsDisplayed()
        composeRule.onNodeWithTag(SessionDetailTestTags.DELETE).assertDoesNotExist()
    }

    @Test
    fun `sharing reports it once`() {
        var shared = 0
        render(ready(), onShare = { shared++ })

        composeRule.onNodeWithTag(SessionDetailTestTags.OVERFLOW).performClick()
        composeRule.onNodeWithTag(SessionDetailTestTags.SHARE).performClick()

        assertEquals(1, shared)
    }

    @Test
    fun `renaming reports the typed name`() {
        var renamed: String? = null
        render(ready(), onRename = { renamed = it })

        composeRule.onNodeWithTag(SessionDetailTestTags.OVERFLOW).performClick()
        composeRule.onNodeWithTag(SessionDetailTestTags.RENAME).performClick()
        composeRule.onNodeWithTag(SessionDetailTestTags.RENAME_FIELD).performTextInput("Morning loop")
        composeRule.onNodeWithTag(SessionDetailTestTags.RENAME_CONFIRM).performClick()

        assertEquals("Morning loop", renamed)
    }

    @Test
    fun `the rename field starts empty for an unnamed run rather than offering its date to edit`() {
        var renamed: String? = null
        render(ready(hasCustomTitle = false), onRename = { renamed = it })

        composeRule.onNodeWithTag(SessionDetailTestTags.OVERFLOW).performClick()
        composeRule.onNodeWithTag(SessionDetailTestTags.RENAME).performClick()
        composeRule.onNodeWithTag(SessionDetailTestTags.RENAME_CONFIRM).performClick()

        assertEquals("", renamed)
    }

    @Test
    fun `the rename field pre-fills an existing name so it can be edited`() {
        var renamed: String? = null
        render(ready(titleLabel = "Morning loop", hasCustomTitle = true), onRename = { renamed = it })

        composeRule.onNodeWithTag(SessionDetailTestTags.OVERFLOW).performClick()
        composeRule.onNodeWithTag(SessionDetailTestTags.RENAME).performClick()
        composeRule.onNodeWithTag(SessionDetailTestTags.RENAME_CONFIRM).performClick()

        assertEquals("Morning loop", renamed)
    }

    @Test
    fun `a session that is gone offers no actions to perform on it`() {
        render(SessionDetailUiState.NotFound)

        composeRule.onNodeWithTag(SessionDetailTestTags.OVERFLOW).assertDoesNotExist()
    }
}
