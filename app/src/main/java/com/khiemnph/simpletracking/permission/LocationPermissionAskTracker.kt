package com.khiemnph.simpletracking.permission

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists whether this app has ever asked the user for [android.Manifest.permission.ACCESS_FINE_LOCATION].
 *
 * Android's own [androidx.fragment.app.Fragment.shouldShowRequestPermissionRationale] returns
 * `false` both before the very first request and after a permanent denial - the two states this
 * app must tell apart to decide whether to show a rationale dialog or a Settings deep-link dialog.
 * A simple persisted flag is the only way to distinguish them, since the platform exposes no
 * direct "have we ever asked" signal.
 */
@Singleton
class LocationPermissionAskTracker @Inject constructor(@ApplicationContext context: Context) {

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasAskedBefore(): Boolean = preferences.getBoolean(KEY_HAS_ASKED, false)

    fun markAsked() = preferences.edit { putBoolean(KEY_HAS_ASKED, true) }

    private companion object {
        const val PREFS_NAME = "location_permission_ask_tracker"
        const val KEY_HAS_ASKED = "has_asked_location_permission"
    }
}
