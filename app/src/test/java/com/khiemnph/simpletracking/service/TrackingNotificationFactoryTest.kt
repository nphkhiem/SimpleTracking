package com.khiemnph.simpletracking.service

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
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
}
