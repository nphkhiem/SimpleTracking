package com.khiemnph.simpletracking.ui.history

import androidx.compose.ui.test.assertIsDisplayed
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
 * Exercises [HistoryScreen] directly, with no ViewModel, database or Fragment. That is the point of
 * keeping the composable stateless: these assertions are about what is rendered from a given list,
 * which is what the old adapter tests covered before the screen moved to Compose.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HistoryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun session(id: String, distance: String) = HistorySummaryUiModel(
        id = id,
        recordedAtLabel = "Sat, 9:41 AM",
        distanceLabel = distance,
        durationLabel = "28:14",
        averageSpeedLabel = "11.1 km/h avg",
        routePoints = emptyList(),
    )

    private fun setScreen(
        sessions: List<HistorySummaryUiModel>,
        onRecordClick: () -> Unit = {},
    ) = composeRule.setContent {
        ChayNgayDiTheme { HistoryScreen(sessions = sessions, onRecordClick = onRecordClick) }
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

        composeRule.onNodeWithTag(HistoryTestTags.RECORD_BUTTON).performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun givenNoSessions_whenScreenRendered_thenRecordButtonIsStillReachable() {
        setScreen(emptyList())

        composeRule.onNodeWithTag(HistoryTestTags.RECORD_BUTTON).assertIsDisplayed()
    }

    @Test
    fun givenSessions_whenScreenRendered_thenDividersSeparateRowsWithNoneAfterTheLast() {
        setScreen(listOf(session("a", "1.00 km"), session("b", "2.00 km"), session("c", "3.00 km")))

        // Three rows have two gaps between them. A third divider would be fencing off empty space
        // below the list rather than separating anything.
        composeRule.onAllNodesWithTag(HistoryTestTags.DIVIDER).assertCountEquals(2)
    }

    @Test
    fun givenASingleSession_whenScreenRendered_thenThereIsNoDividerAtAll() {
        setScreen(listOf(session("only", "5.23 km")))

        composeRule.onAllNodesWithTag(HistoryTestTags.DIVIDER).assertCountEquals(0)
    }

    /** The list must not stop at a screenful: a real history grows past what fits. */
    @Test
    fun givenMoreSessionsThanFitOnScreen_whenScrolledToTheLast_thenItIsRendered() {
        val many = (1..40).map { session("s$it", "$it.00 km") }
        setScreen(many)

        composeRule.onNodeWithTag(HistoryTestTags.LIST).performScrollToIndex(many.lastIndex)

        composeRule.onNodeWithText("40.00 km").assertIsDisplayed()
    }
}
