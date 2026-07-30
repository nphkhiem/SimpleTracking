package com.khiemnph.simpletracking

import android.app.Application
import com.khiemnph.simpletracking.settings.ThemeApplier
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SimpleTrackingApp : Application() {

    /**
     * Started here rather than in an Activity so the stored theme is applied before the first
     * screen is laid out, which is what stops the app flashing the system theme on launch.
     */
    @Inject lateinit var themeApplier: ThemeApplier

    override fun onCreate() {
        super.onCreate()
        themeApplier.start()
    }
}
