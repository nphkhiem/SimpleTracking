package com.khiemnph.simpletracking.utils.extension

import android.util.TypedValue
import com.khiemnph.simpletracking.SimpleTrackingApp

/**
 * Created by Khiem Nguyen on 12/30/2020.
 */

val Int.dp: Int get() = this.toFloat().dp.toInt()

val Float.dp: Float get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this,
        SimpleTrackingApp.appContext.resources.displayMetrics
)

fun Float.toKiloMetreUnit(): Float = this / 1000

fun Float.toTextKilometre(): String = String.format("%.3f(km)", this)

fun Float.toTextKilometrePerHour(): String = String.format("%.1f(km/h)", this)

fun Float.toMetre(): String = String.format("%.3f(m)", this)