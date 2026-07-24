package com.khiemnph.simpletracking.permission

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.khiemnph.simpletracking.R
import javax.inject.Inject

/**
 * Builds the two dialogs [com.khiemnph.simpletracking.ui.record.RecordFragment] shows when
 * `ACCESS_FINE_LOCATION` isn't granted and this isn't the very first-ever ask (the standard
 * guidance is to skip a rationale before that first ask entirely):
 * - [locationRationaleDialog] once the user has denied at least once but can still be re-asked.
 * - [locationPermanentlyDeniedDialog] once the permission is permanently denied and the system
 *   would otherwise silently re-deny a repeat request without even showing a dialog.
 *
 * Plain [AlertDialog.Builder] rather than Material's `MaterialAlertDialogBuilder` - this app's
 * theme ([R.style.AppTheme]) descends from `Theme.AppCompat`, not `Theme.MaterialComponents`,
 * and the latter throws if the theme doesn't supply its color attributes.
 */
class PermissionRationaleDialogFactory @Inject constructor() {

    fun locationRationaleDialog(context: Context, onContinue: () -> Unit): AlertDialog =
        AlertDialog.Builder(context)
            .setTitle(R.string.permission_location_rationale_title)
            .setMessage(R.string.permission_location_rationale_message)
            .setCancelable(false)
            .setPositiveButton(R.string.permission_location_rationale_positive) { _, _ -> onContinue() }
            .setNegativeButton(R.string.permission_location_rationale_negative, null)
            .create()

    fun locationPermanentlyDeniedDialog(context: Context, onOpenSettings: () -> Unit): AlertDialog =
        AlertDialog.Builder(context)
            .setTitle(R.string.permission_location_permanently_denied_title)
            .setMessage(R.string.permission_location_permanently_denied_message)
            .setCancelable(false)
            .setPositiveButton(R.string.permission_location_permanently_denied_positive) { _, _ -> onOpenSettings() }
            .setNegativeButton(R.string.permission_location_permanently_denied_negative, null)
            .create()
}
