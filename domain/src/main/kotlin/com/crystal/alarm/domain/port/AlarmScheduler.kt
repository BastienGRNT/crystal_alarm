package com.crystal.alarm.domain.port

import com.crystal.alarm.domain.model.Alarm

/** Port to the OS alarm system. Implemented in the app layer with AlarmManager. */
interface AlarmScheduler {
    fun schedule(alarm: Alarm)

    /** Fires the alarm right away (e.g. after a reboot while it was ringing). */
    fun fireNow()

    fun cancel()
}
