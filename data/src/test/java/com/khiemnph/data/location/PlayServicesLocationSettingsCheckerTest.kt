package com.khiemnph.data.location

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Tasks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PlayServicesLocationSettingsCheckerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun pendingIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(),
        PendingIntent.FLAG_IMMUTABLE,
    )

    @Test
    fun givenLocationServiceAlreadySatisfied_whenCheck_thenReturnsSatisfied() = runTest {
        val settingsClient = mockk<SettingsClient>()
        every { settingsClient.checkLocationSettings(any<LocationSettingsRequest>()) } returns
            Tasks.forResult(mockk<LocationSettingsResponse>())
        val checker = PlayServicesLocationSettingsChecker(settingsClient)

        val result = checker.check()

        assertEquals(LocationSettingsResult.Satisfied, result)
    }

    @Test
    fun givenLocationServiceOffButResolvable_whenCheck_thenReturnsResolutionRequiredWithTheResolutionIntentSender() = runTest {
        val settingsClient = mockk<SettingsClient>()
        val resolution = pendingIntent()
        val exception = ResolvableApiException(Status(0, "resolution required", resolution))
        every { settingsClient.checkLocationSettings(any<LocationSettingsRequest>()) } returns
            Tasks.forException(exception)
        val checker = PlayServicesLocationSettingsChecker(settingsClient)

        val result = checker.check()

        assertTrue(result is LocationSettingsResult.ResolutionRequired)
        assertEquals(
            resolution.intentSender,
            (result as LocationSettingsResult.ResolutionRequired).intentSender,
        )
    }

    @Test
    fun givenLocationServiceUnresolvable_whenCheck_thenReturnsUnresolvable() = runTest {
        val settingsClient = mockk<SettingsClient>()
        val exception = ApiException(Status(com.google.android.gms.common.api.CommonStatusCodes.ERROR))
        every { settingsClient.checkLocationSettings(any<LocationSettingsRequest>()) } returns
            Tasks.forException(exception)
        val checker = PlayServicesLocationSettingsChecker(settingsClient)

        val result = checker.check()

        assertEquals(LocationSettingsResult.Unresolvable, result)
    }
}
