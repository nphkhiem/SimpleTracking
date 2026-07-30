package com.khiemnph.simpletracking.ui.runs

/**
 * Which heading a day's group of runs sits under.
 *
 * A type rather than a finished string, because "Today" and "Yesterday" are words that have to come
 * from resources, and the grouping logic has no `Context` to look them up with. [Dated] carries an
 * already-formatted date, which the date formatter localises on its own.
 */
sealed interface DayLabel {

    data object Today : DayLabel

    data object Yesterday : DayLabel

    data class Dated(val formattedDate: String) : DayLabel

    /** Stable across recomposition and across a language change, so it is safe as a list key. */
    val key: String
        get() = when (this) {
            Today -> "today"
            Yesterday -> "yesterday"
            is Dated -> formattedDate
        }
}
