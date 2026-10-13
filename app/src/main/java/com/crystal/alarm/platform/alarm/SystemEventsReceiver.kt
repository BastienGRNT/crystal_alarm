package com.crystal.alarm.platform.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.crystal.alarm.di.ReceiverEntryPoint
import com.crystal.alarm.platform.system.launchAsync

/** Re-arms the alarm after reboot, app update, clock or time zone change. */
class SystemEventsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        val entryPoint = ReceiverEntryPoint.get(context)
        launchAsync(entryPoint.applicationScope()) {
            entryPoint.restoreAlarm()()
            entryPoint.widgetUpdater().refresh()
        }
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }
}
