package com.khiemnph.simpletracking.ui.format

import java.util.Locale

private const val METERS_PER_KILOMETER = 1_000.0

/**
 * A distance in metres rendered as kilometres to two decimals, without a unit suffix.
 *
 * [locale] defaults to the device's rather than being pinned, because the decimal separator is not
 * universal: this app's own audience writes `12,50`, not `12.50`. Tests pass an explicit locale so
 * their expectations do not depend on the machine they run on.
 */
fun formatDistanceKm(distanceMeters: Double, locale: Locale = Locale.getDefault()): String =
    String.format(locale, "%.2f", distanceMeters / METERS_PER_KILOMETER)
