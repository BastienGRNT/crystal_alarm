package com.crystal.alarm.platform.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.crystal.alarm.domain.model.Alarm
import com.crystal.alarm.domain.port.AlarmScheduler
import com.crystal.alarm.domain.port.TimeSource
import com.crystal.alarm.presentation.main.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uses [AlarmManager.setAlarmClock]: exact, survives Doze, shows the alarm icon in the
 * status bar, and lets the receiver start a foreground service from the background.
 */
@Singleton
class AndroidAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timeSource: TimeSource,
) : AlarmScheduler {

    private val alarmManager: AlarmManager = requireNotNull(context.getSystemService())

    override fun schedule(alarm: Alarm) = setAlarmClock(alarm.triggerAt.toEpochMilli())

    override fun fireNow() = setAlarmClock(timeSource.now().toEpochMilli() + FIRE_NOW_DELAY_MS)

    override fun cancel() = alarmManager.cancel(firePendingIntent())

    private fun setAlarmClock(triggerAtMillis: Long) {
        val operation = firePendingIntent()
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent()), operation)
        } else {
            // Degraded mode until the user grants exact alarms (Android 12 only).
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
        }
    }

    private fun firePendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_FIRE,
        Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_FIRE),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun showPendingIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_SHOW,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val REQUEST_FIRE = 1
        const val REQUEST_SHOW = 2
        const val FIRE_NOW_DELAY_MS = 1_000L
    }
}
