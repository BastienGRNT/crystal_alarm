package com.crystal.alarm.domain.repository

import com.crystal.alarm.domain.model.Alarm
import com.crystal.alarm.domain.model.AlarmState
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalTime

interface AlarmRepository {
    val state: Flow<AlarmState>

    /** Time of the last alarm ever scheduled, kept by [clear]. Null if there never was one. */
    val lastTime: Flow<LocalTime?>

    suspend fun current(): AlarmState

    suspend fun schedule(alarm: Alarm)

    suspend fun markRinging(alarm: Alarm, since: Instant)

    /** Removes the scheduled or ringing alarm; its [lastTime] stays. */
    suspend fun clear()
}
