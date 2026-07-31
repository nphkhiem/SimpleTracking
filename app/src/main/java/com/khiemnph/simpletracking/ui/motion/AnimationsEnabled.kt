package com.khiemnph.simpletracking.ui.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Reads the system animation setting once per composition that asks for it. */
@Composable
fun rememberAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        MotionPreference.animationsEnabled(MotionPreference.durationScale(context))
    }
}
