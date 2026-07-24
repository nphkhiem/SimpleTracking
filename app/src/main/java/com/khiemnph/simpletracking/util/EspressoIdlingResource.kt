package com.khiemnph.simpletracking.util

import androidx.test.espresso.idling.CountingIdlingResource

/**
 * Backs the [androidx.test.espresso.IdlingResource] instrumented tests register with Espresso so
 * it waits for [com.khiemnph.simpletracking.ui.record.RecordViewModel]'s Flow-driven state
 * updates to fully reach [com.khiemnph.simpletracking.ui.record.RecordFragment] before the next
 * assertion, instead of racing the async hop between [com.khiemnph.simpletracking.service.TrackingService]'s
 * background dispatcher and the Main thread.
 *
 * Lives in production code, not androidTest, because its increment/decrement call sites are
 * inside [com.khiemnph.simpletracking.ui.record.RecordViewModel] itself - androidTest has no other
 * way to hook into that async span. [countingIdlingResource] is only ever registered with
 * `IdlingRegistry` from androidTest; left unregistered, an unused [CountingIdlingResource] costs a
 * couple of atomic-counter operations per call, which is why shipping it in the release APK is
 * standard, low-risk practice.
 *
 * [decrement] is deliberately more lenient than [CountingIdlingResource]'s own default behaviour:
 * [RecordViewModel][com.khiemnph.simpletracking.ui.record.RecordViewModel] increments once per
 * user/test-triggered action but decrements once per resulting state emission, and outside
 * instrumented tests (e.g. the existing Robolectric suite, which drives the fake repository
 * directly and emits several states per action with no matching increment at all) that ratio is
 * never 1:1 - decrementing an already-idle counter would otherwise throw.
 *
 * The idle check and the real decrement below must happen as one atomic step: [isIdleNow] and
 * [CountingIdlingResource.decrement] are two separate, unsynchronized calls against the same
 * underlying [AtomicInteger][java.util.concurrent.atomic.AtomicInteger] counter, so two threads
 * racing this method while the counter sits at 1 could both observe `isIdleNow == false` and both
 * proceed to the real decrement - the second one would then underflow the counter, which
 * [CountingIdlingResource] itself turns into a thrown `IllegalStateException`. `synchronized`
 * (rather than mirroring the counter in a second, parallel `AtomicInteger`) is enough here: nothing
 * outside this object ever calls [CountingIdlingResource.decrement] directly, so serializing every
 * call through this one method fully closes the race without duplicating state that could itself
 * drift out of sync with the real counter.
 */
object EspressoIdlingResource {

    private const val RESOURCE_NAME = "RECORD_VIEWMODEL_STATE_UPDATES"

    val countingIdlingResource: CountingIdlingResource = CountingIdlingResource(RESOURCE_NAME)

    fun increment() {
        countingIdlingResource.increment()
    }

    @Synchronized
    fun decrement() {
        if (!countingIdlingResource.isIdleNow) countingIdlingResource.decrement()
    }
}
