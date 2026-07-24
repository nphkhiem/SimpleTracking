package com.khiemnph.simpletracking.permission

import android.content.Context
import android.os.Looper
import android.view.ContextThemeWrapper
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.R as AppCompatR
import androidx.test.core.app.ApplicationProvider
import com.khiemnph.simpletracking.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PermissionRationaleDialogFactoryTest {

    private val context = ContextThemeWrapper(ApplicationProvider.getApplicationContext<Context>(), R.style.AppTheme)
    private val factory = PermissionRationaleDialogFactory()

    private fun idleMainLooper() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun givenRationaleDialog_whenBuilt_thenShowsLocationRationaleTitleAndMessage() {
        val dialog = factory.locationRationaleDialog(context) {}
        dialog.show()
        idleMainLooper()

        assertEquals(
            context.getString(R.string.permission_location_rationale_title),
            dialog.findViewById<TextView>(AppCompatR.id.alertTitle)?.text.toString(),
        )
        assertEquals(
            context.getString(R.string.permission_location_rationale_message),
            dialog.findViewById<TextView>(android.R.id.message)?.text.toString(),
        )
    }

    @Test
    fun givenRationaleDialog_whenPositiveButtonClicked_thenContinueCallbackInvoked() {
        var continued = false
        val dialog = factory.locationRationaleDialog(context) { continued = true }
        dialog.show()
        idleMainLooper()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        idleMainLooper()

        assertTrue(continued)
    }

    @Test
    fun givenRationaleDialog_whenNegativeButtonClicked_thenContinueCallbackNotInvoked() {
        var continued = false
        val dialog = factory.locationRationaleDialog(context) { continued = true }
        dialog.show()
        idleMainLooper()

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        idleMainLooper()

        assertFalse(continued)
    }

    @Test
    fun givenPermanentlyDeniedDialog_whenBuilt_thenShowsSettingsDeepLinkTitleAndMessage() {
        val dialog = factory.locationPermanentlyDeniedDialog(context) {}
        dialog.show()
        idleMainLooper()

        assertEquals(
            context.getString(R.string.permission_location_permanently_denied_title),
            dialog.findViewById<TextView>(AppCompatR.id.alertTitle)?.text.toString(),
        )
        assertEquals(
            context.getString(R.string.permission_location_permanently_denied_message),
            dialog.findViewById<TextView>(android.R.id.message)?.text.toString(),
        )
    }

    @Test
    fun givenPermanentlyDeniedDialog_whenPositiveButtonClicked_thenOpenSettingsCallbackInvoked() {
        var openedSettings = false
        val dialog = factory.locationPermanentlyDeniedDialog(context) { openedSettings = true }
        dialog.show()
        idleMainLooper()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        idleMainLooper()

        assertTrue(openedSettings)
    }
}
