package com.khiemnph.simpletracking.utils

import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.khiemnph.simpletracking.SimpleTrackingApp

/**
 * Created by Khiem Nguyen on 12/26/2020.
 */
class UIUtil {

    companion object {
        fun getColor(@ColorRes colorResId: Int): Int =
            ContextCompat.getColor(SimpleTrackingApp.appContext, colorResId)

        fun getDrawable(@DrawableRes drawableResId: Int): Drawable? =
            ContextCompat.getDrawable(SimpleTrackingApp.appContext, drawableResId)

        fun screenWidth(): Int = SimpleTrackingApp.appContext.resources.displayMetrics.widthPixels
    }
}