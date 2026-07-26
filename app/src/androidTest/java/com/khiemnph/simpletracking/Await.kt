package com.khiemnph.simpletracking

import android.os.SystemClock
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matcher

private const val DEFAULT_TIMEOUT_MILLIS = 5_000L
private const val POLL_INTERVAL_MILLIS = 50L

/**
 * Retries an Espresso assertion until it holds, or fails with the last error.
 *
 * This replaces the `CountingIdlingResource` that used to live in production code, incremented and
 * decremented from inside `MainActivity` and `RecordViewModel` so instrumented tests could wait on
 * their async spans. Shipping test synchronisation in the release APK to serve two test files was
 * a poor trade, and it shaped the ViewModel's internals around the counter's accounting: the
 * awkward increment-per-action, decrement-per-emission ratio, and a `try`/`catch`-with-rethrow that
 * existed only to keep that ratio balanced.
 *
 * Retrying the assertion buys the same thing from the test side. An idling resource tells Espresso
 * *when* to look; this simply looks until the answer is the expected one. It needs no cooperation
 * from the code under test, which means it cannot go stale when that code changes shape.
 */
fun awaitView(
    viewId: Int,
    matcher: Matcher<View>,
    timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
) {
    awaitAssertion(timeoutMillis) { onView(withId(viewId)).check(matches(matcher)) }
}

/** Retries [assertion] until it stops throwing, then rethrows the last failure if it never does. */
fun awaitAssertion(timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS, assertion: () -> Unit) {
    val deadline = SystemClock.uptimeMillis() + timeoutMillis
    var lastError: Throwable
    while (true) {
        try {
            assertion()
            return
        } catch (error: Throwable) {
            lastError = error
        }
        if (SystemClock.uptimeMillis() >= deadline) throw lastError
        SystemClock.sleep(POLL_INTERVAL_MILLIS)
    }
}

/** Retries [supplier] until it returns a non-null value, or fails after [timeoutMillis]. */
fun <T : Any> awaitNotNull(
    description: String,
    timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    supplier: () -> T?,
): T {
    val deadline = SystemClock.uptimeMillis() + timeoutMillis
    while (true) {
        supplier()?.let { return it }
        check(SystemClock.uptimeMillis() < deadline) { "Timed out waiting for $description" }
        SystemClock.sleep(POLL_INTERVAL_MILLIS)
    }
}
