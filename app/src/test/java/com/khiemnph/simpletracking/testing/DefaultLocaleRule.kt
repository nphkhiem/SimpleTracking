package com.khiemnph.simpletracking.testing

import java.util.Locale
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Pins the JVM default locale for the duration of a test, then restores it.
 *
 * The app's formatters resolve the device locale on purpose, which is what makes a Vietnamese
 * reader see `12,50 km`. That leaves any test asserting an exact formatted string dependent on the
 * machine it runs on: the same assertion passes in California and fails in Hanoi. Pinning here
 * makes the expectation explicit rather than accidental.
 */
class DefaultLocaleRule(private val locale: Locale = Locale.US) : TestWatcher() {

    private lateinit var original: Locale

    override fun starting(description: Description) {
        original = Locale.getDefault()
        Locale.setDefault(locale)
    }

    override fun finished(description: Description) {
        Locale.setDefault(original)
    }
}
