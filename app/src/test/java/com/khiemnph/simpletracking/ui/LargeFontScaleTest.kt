package com.khiemnph.simpletracking.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.khiemnph.domain.model.LatLngPoint
import com.khiemnph.simpletracking.settings.ThemeChoice
import com.khiemnph.simpletracking.testing.DefaultLocaleRule
import com.khiemnph.simpletracking.ui.components.MetricGridTestTags
import com.khiemnph.simpletracking.ui.detail.SessionDetailScreen
import com.khiemnph.simpletracking.ui.detail.SessionDetailTestTags
import com.khiemnph.simpletracking.ui.detail.SessionDetailUiState
import com.khiemnph.simpletracking.ui.detail.SplitUiModel
import com.khiemnph.simpletracking.ui.runs.DayLabel
import com.khiemnph.simpletracking.ui.runs.RunSummaryUiModel
import com.khiemnph.simpletracking.ui.runs.RunsScreen
import com.khiemnph.simpletracking.ui.runs.RunsTestTags
import com.khiemnph.simpletracking.ui.runs.RunsUiState
import com.khiemnph.simpletracking.ui.runs.SessionGroupUiModel
import com.khiemnph.simpletracking.ui.runs.WeekSummaryUiModel
import com.khiemnph.simpletracking.ui.settings.SettingsScreen
import com.khiemnph.simpletracking.ui.settings.SettingsTestTags
import com.khiemnph.simpletracking.ui.settings.SettingsUiState
import com.khiemnph.simpletracking.ui.summary.SummaryScreen
import com.khiemnph.simpletracking.ui.summary.SummaryTestTags
import com.khiemnph.simpletracking.ui.summary.SummaryUiState
import com.khiemnph.simpletracking.ui.theme.ChayNgayDiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Every Compose screen at 150 percent font scale.
 *
 * The brief asks the app to look intentional at `font_scale 1.5`, and this design leans on
 * oversized tabular numerals, which is exactly where enlarging text runs out of room first. These
 * assert the primary action of each screen is still reachable, since a control pushed off the
 * bottom is the failure that actually strands someone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], fontScale = 1.5f)
class LargeFontScaleTest {

    @get:Rule
    val localeRule = DefaultLocaleRule()

    @get:Rule
    val composeRule = createComposeRule()

    private val route = listOf(
        LatLngPoint(21.0278, 105.8342),
        LatLngPoint(21.0288, 105.8342),
        LatLngPoint(21.0288, 105.8352),
    )

    private val week = WeekSummaryUiModel(
        distanceKm = "128.50",
        runCount = 12,
        durationLabel = "11:12:00",
        dailyDistanceFractions = listOf(0f, 0.4f, 0f, 1f, 0f, 0.6f, 0.2f),
    )

    private fun run(id: String) = RunSummaryUiModel(
        id = id,
        recordedAtLabel = "Thứ Hai, 9:41 SA",
        distanceKm = "128.50",
        durationLabel = "11:12:00",
        averageSpeedKmh = "11.1",
        routePoints = route,
    )

    @Test
    fun `Runs keeps its record button reachable`() {
        composeRule.setContent {
            ChayNgayDiTheme {
                RunsScreen(
                    state = RunsUiState.Sessions(week, listOf(SessionGroupUiModel(DayLabel.Today, listOf(run("a"))))),
                    onRecordClick = {},
                    onSessionClick = {},
                    onSettingsClick = {},
                    onSessionSwipedAway = {},
                    onUndoDelete = {},
                )
            }
        }

        composeRule.onNodeWithTag(RunsTestTags.RECORD_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(RunsTestTags.SETTINGS_BUTTON).assertIsDisplayed()
    }

    @Test
    fun `Summary keeps both of its actions reachable`() {
        composeRule.setContent {
            ChayNgayDiTheme {
                SummaryScreen(
                    state = SummaryUiState.Ready(
                        distanceKm = "128.50",
                        durationLabel = "11:12:00",
                        paceLabel = "5:12",
                        routePoints = route,
                        isTooShortToKeep = false,
                    ),
                    onKeep = {},
                    onDiscard = {},
                )
            }
        }

        composeRule.onNodeWithTag(SummaryTestTags.KEEP).assertIsDisplayed()
        composeRule.onNodeWithTag(SummaryTestTags.DISCARD).assertIsDisplayed()
    }

    @Test
    fun `Session detail can still reach its metrics and delete`() {
        composeRule.setContent {
            ChayNgayDiTheme {
                SessionDetailScreen(
                    state = SessionDetailUiState.Ready(
                        titleLabel = "Thứ Bảy, 9 tháng 8 · 9:41 SA",
                        hasCustomTitle = false,
                        distanceKm = "128.50",
                        durationLabel = "11:12:00",
                        averagePaceLabel = "5:12",
                        bestPaceLabel = "4:48",
                        routePoints = route,
                        splits = listOf(SplitUiModel("1", "5:12", 1f, isFastest = true)),
                    ),
                    onBack = {},
                    onRename = {},
                    onShare = {},
                    onDelete = {},
                )
            }
        }

        composeRule.onNodeWithTag(SessionDetailTestTags.SPLITS)
            .performScrollToNode(hasTestTag(MetricGridTestTags.metricFor("best pace")))
        composeRule.onNodeWithTag(MetricGridTestTags.metricFor("best pace")).assertIsDisplayed()

        composeRule.onNodeWithTag(SessionDetailTestTags.SPLITS)
            .performScrollToNode(hasTestTag(SessionDetailTestTags.DELETE))
        composeRule.onNodeWithTag(SessionDetailTestTags.DELETE).assertIsDisplayed()
    }

    @Test
    fun `Settings can still reach the version at the bottom`() {
        composeRule.setContent {
            ChayNgayDiTheme {
                SettingsScreen(
                    state = SettingsUiState(theme = ThemeChoice.System, versionName = "1.0"),
                    onThemeChosen = {},
                    onDynamicColourChanged = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag(SettingsTestTags.VERSION).performScrollTo().assertIsDisplayed()
    }
}
