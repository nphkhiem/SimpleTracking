package com.khiemnph.simpletracking.permission

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
 * Uses [MaterialAlertDialogBuilder] so these dialogs pick up the app's Material 3 colour roles,
 * shape and typography. That was previously not possible: the old theme descended from
 * `Theme.AppCompat`, and this builder throws when the theme does not supply Material's colour
 * attributes. `Theme.ChayNgayDi` does, so the dialogs no longer look like leftovers from a
 * different design system.
 */
class PermissionRationaleDialogFactory @Inject constructor() {

    fun locationRationaleDialog(context: Context, onContinue: () -> Unit): AlertDialog =
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.permission_location_rationale_title)
            .setMessage(R.string.permission_location_rationale_message)
            .setCancelable(false)
            .setPositiveButton(R.string.permission_location_rationale_positive) { _, _ -> onContinue() }
            .setNegativeButton(R.string.permission_location_rationale_negative, null)
            .create()

    fun locationPermanentlyDeniedDialog(context: Context, onOpenSettings: () -> Unit): AlertDialog =
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.permission_location_permanently_denied_title)
            .setMessage(R.string.permission_location_permanently_denied_message)
            .setCancelable(false)
            .setPositiveButton(R.string.permission_location_permanently_denied_positive) { _, _ -> onOpenSettings() }
            .setNegativeButton(R.string.permission_location_permanently_denied_negative, null)
            .create()
}
