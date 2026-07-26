package com.khiemnph.simpletracking.service

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.khiemnph.simpletracking.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TrackingNotificationFactoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val factory = TrackingNotificationFactory(context)

    @Test
    fun givenRunningState_whenBuildNotificationCalled_thenContentTextReflectsRunning() {
        val notification = factory.buildNotification("session-1", isPaused = false)

        assertEquals("Recording your route", notification.extras.getCharSequence(Notification.EXTRA_TEXT))
    }

    @Test
    fun givenPausedState_whenBuildNotificationCalled_thenContentTextReflectsPaused() {
        val notification = factory.buildNotification("session-1", isPaused = true)

        assertEquals("Paused", notification.extras.getCharSequence(Notification.EXTRA_TEXT))
    }

    @Test
    fun givenAnyState_whenBuildNotificationCalled_thenNotificationIsOngoingWithPauseResumeAndStopActions() {
        val notification = factory.buildNotification("session-1", isPaused = false)

        assertTrue((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0)
        assertEquals(2, notification.actions.size)
    }

    @Test
    fun givenRunningState_whenBuildNotificationCalled_thenFirstActionOffersPause() {
        val notification = factory.buildNotification("session-1", isPaused = false)

        assertEquals("Pause", notification.actions.first().title)
    }

    @Test
    fun givenPausedState_whenBuildNotificationCalled_thenFirstActionOffersResume() {
        val notification = factory.buildNotification("session-1", isPaused = true)

        assertEquals("Resume", notification.actions.first().title)
    }

    @Test
    fun givenUpdateNotificationCalled_thenNotificationIsPostedUnderTheSharedNotificationId() {
        factory.updateNotification("session-1", isPaused = false)

        val manager = context.getSystemService(NotificationManager::class.java)
        val active = manager.activeNotifications.map { it.id }

        assertTrue(active.contains(TrackingNotificationFactory.NOTIFICATION_ID))
    }

    @Test
    fun givenBuildNotificationCalled_thenNotificationChannelIsCreated() {
        factory.buildNotification("session-1", isPaused = false)

        val manager = context.getSystemService(NotificationManager::class.java)

        assertTrue(manager.notificationChannels.isNotEmpty())
    }

    @Test
    fun `small icon is a monochrome status bar glyph, not the launcher mipmap`() {
        val notification = factory.buildNotification("session-1", isPaused = false)

        // A full-colour launcher icon is drawn as a featureless white blob in the status bar,
        // because the system masks the small icon to its alpha channel.
        assertNotEquals(R.mipmap.ic_launcher, notification.smallIcon.resId)
        assertEquals(R.drawable.ic_stat_tracking, notification.smallIcon.resId)
    }

    @Test
    fun `tapping the notification opens the app instead of doing nothing`() {
        val notification = factory.buildNotification("session-1", isPaused = false)

        assertNotNull("a tracking notification with no contentIntent is a dead tap", notification.contentIntent)
    }

    @Test
    fun `every action carries an icon for surfaces that render them`() {
        val notification = factory.buildNotification("session-1", isPaused = false)

        notification.actions.forEach { action ->
            assertNotEquals("action '${action.title}' has no icon", 0, action.icon)
        }
    }
}
