package com.crystal.alarm.platform.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.crystal.alarm.R
import com.crystal.alarm.presentation.ringing.RingingActivity

object AlarmNotifications {
    const val RINGING_NOTIFICATION_ID = 42
    private const val CHANNEL_RINGING = "alarm_ringing"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_RINGING,
            context.getString(R.string.notification_channel_ringing),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_ringing_description)
            // Sound and vibration are driven by AlarmSoundPlayer, at alarm volume.
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }

    /** Ongoing, no actions: there is deliberately no "snooze" nor "dismiss" button. */
    fun ringing(context: Context): Notification {
        val openRinging = PendingIntent.getActivity(
            context,
            0,
            Intent(context, RingingActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, CHANNEL_RINGING)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(context.getString(R.string.notification_ringing_title))
            .setContentText(context.getString(R.string.notification_ringing_text))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openRinging)
            .setFullScreenIntent(openRinging, true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
}
