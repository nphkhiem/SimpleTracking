package com.khiemnph.simpletracking.ui.motion

import android.content.Context
import android.provider.Settings

/** Returned when the system setting cannot be read, so it is not confused with a real zero. */
const val UNKNOWN_DURATION_SCALE = -1f

/**
 * Whether the app should animate, and for how long.
 *
 * Not the app's decision to make: someone who turns animations off system-wide has asked every app
 * to stop, and motion sickness is the usual reason. The scale is read once per call rather than
 * cached, because it can change while the app is running.
 */
object MotionPreference {

    fun durationScale(context: Context): Float = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        UNKNOWN_DURATION_SCALE,
    )

    fun animationsEnabled(durationScale: Float): Boolean = durationScale != 0f

    fun scaledDurationMillis(baseMillis: Int, durationScale: Float): Int = when {
        durationScale < 0f -> baseMillis
        else -> (baseMillis * durationScale).toInt()
    }
}
