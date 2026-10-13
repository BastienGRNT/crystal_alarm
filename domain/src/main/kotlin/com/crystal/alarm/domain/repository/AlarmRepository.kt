package com.crystal.alarm.domain.repository

import com.crystal.alarm.domain.model.Alarm
import com.crystal.alarm.domain.model.AlarmState
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface AlarmRepository {
    val state: Flow<AlarmState>

    suspend fun current(): AlarmState

    suspend fun schedule(alarm: Alarm)

    suspend fun markRinging(alarm: Alarm, since: Instant)

    suspend fun clear()
}
