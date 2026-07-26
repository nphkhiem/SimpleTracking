package com.khiemnph.domain.model

/**
 * How much the distance on screen can currently be trusted.
 *
 * Every tracker reports distance as a fact. None of them say when they were guessing, even though
 * independent measurement puts typical phone GPS error near 8 percent and far worse in an urban
 * canyon. The app already records `horizontalAccuracyMeters` for every fix and already knows when
 * the last one arrived, so it can be honest about this at no extra cost.
 *
 * [LOST] is the one that matters most: without it a session whose provider quietly stopped keeps
 * advancing its timer and its notification says "Recording your route" while the route stands still.
 */
enum class GpsSignal {

    /** No fix accepted recently enough to believe the route is still being drawn. */
    LOST,

    /** Fixes are arriving, but accurate enough only to be treated with suspicion. */
    WEAK,

    GOOD,

    /** No fix has been accepted yet, so there is nothing to judge. */
    ACQUIRING,
}
