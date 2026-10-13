package com.crystal.alarm.platform.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fired by AlarmManager. Starts the ringing service right away (exempt from FGS restrictions). */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_FIRE) AlarmRingingService.start(context)
    }

    companion object {
        const val ACTION_FIRE = "com.crystal.alarm.action.FIRE"
    }
}
