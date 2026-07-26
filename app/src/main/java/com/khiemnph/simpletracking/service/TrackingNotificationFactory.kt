package com.khiemnph.simpletracking.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.khiemnph.simpletracking.R
import com.khiemnph.simpletracking.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Builds and updates the persistent notification for [TrackingService]'s foreground state.
 * A static, informative title/text per state is all this phase requires - no live-ticking
 * distance/speed/duration inside the notification itself.
 */
class TrackingNotificationFactory @Inject constructor(@ApplicationContext private val context: Context) {

    private var channelCreated = false

    fun buildNotification(sessionId: String, isPaused: Boolean): Notification {
        ensureChannel()

        val pauseResumeAction = if (isPaused) {
            action(
                R.drawable.ic_play,
                context.getString(R.string.tracking_notification_action_resume),
                TrackingService.resumeIntent(context, sessionId),
                REQUEST_CODE_PAUSE_RESUME,
            )
        } else {
            action(
                R.drawable.ic_pause,
                context.getString(R.string.tracking_notification_action_pause),
                TrackingService.pauseIntent(context, sessionId),
                REQUEST_CODE_PAUSE_RESUME,
            )
        }
        val stopAction = action(
            R.drawable.ic_stop,
            context.getString(R.string.tracking_notification_action_stop),
            TrackingService.stopIntent(context, sessionId),
            REQUEST_CODE_STOP,
        )
        val contentText = context.getString(
            if (isPaused) R.string.tracking_notification_text_paused else R.string.tracking_notification_text_running,
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.tracking_notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_stat_tracking)
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(pauseResumeAction)
            .addAction(stopAction)
            .build()
    }

    /** Rebuilds and re-posts the notification to reflect a new pause/running state. */
    fun updateNotification(sessionId: String, isPaused: Boolean) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(sessionId, isPaused))
    }

    private fun ensureChannel() {
        if (channelCreated) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.tracking_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        channelCreated = true
    }

    /**
     * Reopens the app when the notification body is tapped. `FLAG_ACTIVITY_CLEAR_TOP` reuses the
     * existing task rather than stacking a second MainActivity on top of the running session.
     */
    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_CODE_OPEN_APP,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun action(icon: Int, title: String, intent: Intent, requestCode: Int): NotificationCompat.Action {
        val pendingIntent = PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Action(icon, title, pendingIntent)
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "tracking_channel"
        private const val REQUEST_CODE_PAUSE_RESUME = 100
        private const val REQUEST_CODE_STOP = 101
        private const val REQUEST_CODE_OPEN_APP = 102
    }
}
