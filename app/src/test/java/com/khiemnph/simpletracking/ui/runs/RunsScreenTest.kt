package com.khiemnph.simpletracking.ui.runs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import com.khiemnph.simpletracking.ui.theme.ChayNgayDiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Exercises [RunsScreen] directly, with no ViewModel, database or Fragment. That is the point of
 * keeping the composable stateless: these assertions are about what is rendered from a given list,
 * which is what the old adapter tests covered before the screen moved to Compose.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RunsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun session(id: String, distance: String) = RunSummaryUiModel(
        id = id,
        recordedAtLabel = "Sat, 9:41 AM",
        distanceLabel = distance,
        durationLabel = "28:14",
        averageSpeedLabel = "11.1 km/h avg",
        routePoints = emptyList(),
    )

    private val week = WeekSummaryUiModel(
        distanceLabel = "12.50 km",
        runCountLabel = "3 runs",
        durationLabel = "1:12:00",
        dailyDistanceFractions = listOf(0f, 0.4f, 0f, 1f, 0f, 0.6f, 0.2f),
    )

    private fun setScreen(
        sessions: List<RunSummaryUiModel>,
        onRecordClick: () -> Unit = {},
        onSessionSwipedAway: (String) -> Unit = {},
    ) = setState(
        state = if (sessions.isEmpty()) {
            RunsUiState.Empty
        } else {
            RunsUiState.Sessions(week, listOf(SessionGroupUiModel("Today", sessions)))
        },
        onRecordClick = onRecordClick,
        onSessionSwipedAway = onSessionSwipedAway,
    )

    private fun setState(
        state: RunsUiState,
        onRecordClick: () -> Unit = {},
        onSessionSwipedAway: (String) -> Unit = {},
    ) = composeRule.setContent {
        ChayNgayDiTheme {
            RunsScreen(
                state = state,
                onRecordClick = onRecordClick,
                onSessionSwipedAway = onSessionSwipedAway,
                onUndoDelete = {},
            )
        }
    }

    @Test
    fun givenSessions_whenScreenRendered_thenEachSessionsFormattedLabelsAreShown() {
        setScreen(listOf(session("a", "5.23 km")))

        composeRule.onNodeWithText("5.23 km").assertIsDisplayed()
        composeRule.onNodeWithText("Sat, 9:41 AM").assertIsDisplayed()
        composeRule.onNodeWithText("28:14 · 11.1 km/h avg").assertIsDisplayed()
    }

    @Test
    fun givenRecordButtonTapped_whenClicked_thenCallbackInvokedExactlyOnce() {
        var clicks = 0
        setScreen(sessions = emptyList(), onRecordClick = { clicks++ })

        composeRule.onNodeWithTag(RunsTestTags.RECORD_BUTTON).performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun givenNoSessions_whenScreenRendered_thenRecordButtonIsStillReachable() {
        setScreen(emptyList())

        composeRule.onNodeWithTag(RunsTestTags.RECORD_BUTTON).assertIsDisplayed()
    }

    @Test
    fun givenSessions_whenScreenRendered_thenTheWeekSummaryIsShownAboveThem() {
        setScreen(listOf(session("a", "5.23 km")))

        composeRule.onNodeWithTag(RunsTestTags.WEEK_SUMMARY).assertIsDisplayed()
        composeRule.onNodeWithText("12.50 km").assertIsDisplayed()
    }

    @Test
    fun givenSessionsAcrossTwoDays_whenScreenRendered_thenEachDayHasItsOwnHeading() {
        setState(
            RunsUiState.Sessions(
                week = week,
                groups = listOf(
                    SessionGroupUiModel("Today", listOf(session("a", "5.23 km"))),
                    SessionGroupUiModel("Yesterday", listOf(session("b", "3.56 km"))),
                ),
            ),
        )

        composeRule.onNodeWithTag(RunsTestTags.groupHeaderFor("Today")).assertIsDisplayed()
        composeRule.onNodeWithTag(RunsTestTags.groupHeaderFor("Yesterday")).assertIsDisplayed()
    }

    /** A heading already separates two days, so a rule under a group's last row would be noise. */
    @Test
    fun givenTwoGroupsOfOne_whenScreenRendered_thenThereAreNoDividersAtAll() {
        setState(
            RunsUiState.Sessions(
                week = week,
                groups = listOf(
                    SessionGroupUiModel("Today", listOf(session("a", "5.23 km"))),
                    SessionGroupUiModel("Yesterday", listOf(session("b", "3.56 km"))),
                ),
            ),
        )

        composeRule.onAllNodesWithTag(RunsTestTags.DIVIDER).assertCountEquals(0)
    }

    @Test
    fun givenTheDatabaseHasNotAnsweredYet_whenScreenRendered_thenASkeletonHoldsTheLayout() {
        setState(RunsUiState.Loading)

        composeRule.onNodeWithTag(RunsTestTags.SKELETON).assertIsDisplayed()
    }

    @Test
    fun givenNoSessions_whenScreenRendered_thenTheEmptyStateExplainsWhatToDo() {
        setScreen(emptyList())

        composeRule.onNodeWithTag(RunsTestTags.EMPTY).assertIsDisplayed()
    }

    /**
     * Loading must not render the empty state. Otherwise a returning user is told they have no runs
     * for the moment it takes the database to answer, which is the bug this state exists to avoid.
     */
    /** Loading must never show the empty state: a returning user would be told they have no runs. */
    @Test
    fun givenTheDatabaseHasNotAnsweredYet_whenScreenRendered_thenNoEmptyStateIsShown() {
        setState(RunsUiState.Loading)

        composeRule.onNodeWithTag(RunsTestTags.EMPTY).assertDoesNotExist()
        composeRule.onNodeWithTag(RunsTestTags.LIST).assertDoesNotExist()
    }

    @Test
    fun givenARowSwipedAway_whenDismissed_thenTheSessionIdIsReported() {
        var swiped: String? = null
        setScreen(listOf(session("swipe-me", "5.23 km")), onSessionSwipedAway = { swiped = it })

        composeRule.onNodeWithTag(RunsTestTags.rowFor("swipe-me")).performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        assertEquals("swipe-me", swiped)
    }

    @Test
    fun givenSessions_whenScreenRendered_thenDividersSeparateRowsWithNoneAfterTheLast() {
        setScreen(listOf(session("a", "1.00 km"), session("b", "2.00 km"), session("c", "3.00 km")))

        // Three rows have two gaps between them. A third divider would be fencing off empty space
        // below the list rather than separating anything.
        composeRule.onAllNodesWithTag(RunsTestTags.DIVIDER).assertCountEquals(2)
    }

    @Test
    fun givenASingleSession_whenScreenRendered_thenThereIsNoDividerAtAll() {
        setScreen(listOf(session("only", "5.23 km")))

        composeRule.onAllNodesWithTag(RunsTestTags.DIVIDER).assertCountEquals(0)
    }

    /** The list must not stop at a screenful: a real history grows past what fits. */
    @Test
    fun givenMoreSessionsThanFitOnScreen_whenScrolledToTheLast_thenItIsRendered() {
        val many = (1..40).map { session("s$it", "$it.00 km") }
        setScreen(many)

        composeRule.onNodeWithTag(RunsTestTags.LIST).performScrollToIndex(many.lastIndex)

        composeRule.onNodeWithText("40.00 km").assertIsDisplayed()
    }
}
