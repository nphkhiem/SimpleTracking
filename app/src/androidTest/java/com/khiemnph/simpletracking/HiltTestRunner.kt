package com.khiemnph.simpletracking

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Swaps in [HiltTestApplication] for the real [SimpleTrackingApp] so `@HiltAndroidTest`/
 * `@AndroidEntryPoint` resolve correctly for every class under `androidTest` - wired as
 * `testInstrumentationRunner` in `app/build.gradle`.
 */
class HiltTestRunner : AndroidJUnitRunner() {

    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
