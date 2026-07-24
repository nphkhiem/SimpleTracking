package com.khiemnph.simpletracking.util

import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [EspressoIdlingResource] is a JVM-wide singleton object, so its state can carry over between
 * test methods/classes sharing the same test JVM (Gradle's default, no `forkEvery` configured for
 * this module). [drainToIdle] defensively resets it to a known idle baseline before each test
 * here, independent of whatever earlier tests left behind, so these tests assert only on the
 * behaviour this class introduces rather than on incidental suite ordering.
 *
 * Runs under Robolectric (not plain JUnit) because [CountingIdlingResource][androidx.test.espresso.idling.CountingIdlingResource]'s
 * constructor calls into `android.text.TextUtils`, which the plain "not mocked" Android stub jar
 * throws for outside a Robolectric-provided implementation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class EspressoIdlingResourceTest {

    @Before
    fun drainToIdle() {
        var guard = 0
        while (!EspressoIdlingResource.countingIdlingResource.isIdleNow) {
            EspressoIdlingResource.decrement()
            guard++
            check(guard < 10_000) { "Failed to drain EspressoIdlingResource to idle before the test" }
        }
    }

    /**
     * Regression test for the non-atomic `if (!isIdleNow) decrement()` guard: with the counter at
     * 1, many threads racing `decrement()` simultaneously used to be able to all observe
     * `isIdleNow == false` and all proceed to the real decrement, underflowing the counter and
     * throwing `IllegalStateException` from inside `CountingIdlingResource` itself. The fix makes
     * the check-then-act atomic, so only one of these concurrent calls may actually decrement.
     */
    @Test
    fun givenCounterAtOne_whenDecrementCalledConcurrently_thenExactlyOneCallDecrementsAndNoneThrow() {
        EspressoIdlingResource.increment()

        val threadCount = 64
        val readyLatch = CountDownLatch(threadCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)
        val failures = Collections.synchronizedList(mutableListOf<Throwable>())
        val executor = Executors.newFixedThreadPool(threadCount)

        try {
            repeat(threadCount) {
                executor.execute {
                    readyLatch.countDown()
                    startLatch.await()
                    try {
                        EspressoIdlingResource.decrement()
                    } catch (t: Throwable) {
                        failures.add(t)
                    } finally {
                        doneLatch.countDown()
                    }
                }
            }

            assertTrue("Threads failed to start in time", readyLatch.await(5, TimeUnit.SECONDS))
            startLatch.countDown()
            assertTrue("Concurrent decrement() calls did not finish in time", doneLatch.await(5, TimeUnit.SECONDS))
        } finally {
            executor.shutdown()
        }

        assertTrue(
            "Expected no exception from any of $threadCount concurrent decrement() calls, got: $failures",
            failures.isEmpty(),
        )
        assertTrue(
            "Expected the single balancing decrement among $threadCount concurrent calls to bring the " +
                "resource back to idle",
            EspressoIdlingResource.countingIdlingResource.isIdleNow,
        )
    }
}
