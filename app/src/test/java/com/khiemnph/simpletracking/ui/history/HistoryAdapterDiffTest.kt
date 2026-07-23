package com.khiemnph.simpletracking.ui.history

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryAdapterDiffTest {

    private fun uiModel(
        id: String,
        recordedAtLabel: String = "Tue, 7:12 AM",
        distanceLabel: String = "5.42 km",
        durationLabel: String = "32:18",
        averageSpeedLabel: String = "10.1 km/h avg",
        thumbnailPath: String? = null,
    ) = HistorySummaryUiModel(id, recordedAtLabel, distanceLabel, durationLabel, averageSpeedLabel, thumbnailPath)

    @Test
    fun givenSameId_whenAreItemsTheSameChecked_thenReturnsTrueEvenIfContentDiffers() {
        val oldItem = uiModel(id = "session-1", distanceLabel = "5.42 km")
        val newItem = uiModel(id = "session-1", distanceLabel = "9.10 km")

        assertTrue(HistorySummaryDiffCallback.areItemsTheSame(oldItem, newItem))
    }

    @Test
    fun givenDifferentId_whenAreItemsTheSameChecked_thenReturnsFalse() {
        val oldItem = uiModel(id = "session-1")
        val newItem = uiModel(id = "session-2")

        assertFalse(HistorySummaryDiffCallback.areItemsTheSame(oldItem, newItem))
    }

    @Test
    fun givenIdenticalItems_whenAreContentsTheSameChecked_thenReturnsTrue() {
        val oldItem = uiModel(id = "session-1")
        val newItem = uiModel(id = "session-1")

        assertTrue(HistorySummaryDiffCallback.areContentsTheSame(oldItem, newItem))
    }

    @Test
    fun givenSameIdButDifferentDistanceLabel_whenAreContentsTheSameChecked_thenReturnsFalse() {
        val oldItem = uiModel(id = "session-1", distanceLabel = "5.42 km")
        val newItem = uiModel(id = "session-1", distanceLabel = "9.10 km")

        assertFalse(HistorySummaryDiffCallback.areContentsTheSame(oldItem, newItem))
    }

    @Test
    fun givenTwoListsDifferingByOneItem_whenDiffCalculated_thenOnlyThatItemFlaggedChanged() {
        val oldList = listOf(
            uiModel(id = "session-1", distanceLabel = "5.42 km"),
            uiModel(id = "session-2", distanceLabel = "3.10 km"),
            uiModel(id = "session-3", distanceLabel = "8.75 km"),
        )
        val newList = listOf(
            oldList[0],
            uiModel(id = "session-2", distanceLabel = "4.20 km"),
            oldList[2],
        )

        assertTrue(HistorySummaryDiffCallback.areItemsTheSame(oldList[0], newList[0]))
        assertTrue(HistorySummaryDiffCallback.areContentsTheSame(oldList[0], newList[0]))

        assertTrue(HistorySummaryDiffCallback.areItemsTheSame(oldList[1], newList[1]))
        assertFalse(HistorySummaryDiffCallback.areContentsTheSame(oldList[1], newList[1]))

        assertTrue(HistorySummaryDiffCallback.areItemsTheSame(oldList[2], newList[2]))
        assertTrue(HistorySummaryDiffCallback.areContentsTheSame(oldList[2], newList[2]))
    }
}
