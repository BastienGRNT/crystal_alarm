package com.crystal.alarm.platform.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import com.crystal.alarm.di.ReceiverEntryPoint
import com.crystal.alarm.platform.system.launchAsync

/**
 * Refreshes the widget countdown on each minute boundary with a non-wakeup RTC alarm:
 * delivered only while the device is awake, so it costs nothing while the phone sleeps.
 */
object WidgetTicker {
    private const val REQUEST_TICK = 10

    fun scheduleNextMinute(context: Context, nowMillis: Long) {
        val alarmManager = context.getSystemService<AlarmManager>() ?: return
        val next = (nowMillis / 60_000 + 1) * 60_000
        try {
            alarmManager.setExact(AlarmManager.RTC, next, pendingIntent(context))
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC, next, pendingIntent(context))
        }
    }

    fun cancel(context: Context) {
        context.getSystemService<AlarmManager>()?.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context) = PendingIntent.getBroadcast(
        context,
        REQUEST_TICK,
        Intent(context, WidgetTickReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

class WidgetTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val entryPoint = ReceiverEntryPoint.get(context)
        launchAsync(entryPoint.applicationScope()) { entryPoint.widgetUpdater().refresh() }
    }
}
