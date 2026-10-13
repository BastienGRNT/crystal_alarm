package com.crystal.alarm.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.crystal.alarm.data.local.PreferenceKeys
import com.crystal.alarm.data.local.safeData
import com.crystal.alarm.domain.model.Alarm
import com.crystal.alarm.domain.model.AlarmState
import com.crystal.alarm.domain.repository.AlarmRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreAlarmRepository @Inject constructor(
    private val store: DataStore<Preferences>,
) : AlarmRepository {

    override val state: Flow<AlarmState> = store.safeData
        .map { it.toAlarmState() }
        .distinctUntilChanged()

    override suspend fun current(): AlarmState = state.first()

    override suspend fun schedule(alarm: Alarm) {
        store.edit {
            it.putAlarm(alarm)
            it.remove(PreferenceKeys.ALARM_RINGING_SINCE)
        }
    }

    override suspend fun markRinging(alarm: Alarm, since: Instant) {
        store.edit {
            it.putAlarm(alarm)
            it[PreferenceKeys.ALARM_RINGING_SINCE] = since.toEpochMilli()
        }
    }

    override suspend fun clear() {
        store.edit {
            it.remove(PreferenceKeys.ALARM_MINUTE_OF_DAY)
            it.remove(PreferenceKeys.ALARM_REPEAT_DAYS)
            it.remove(PreferenceKeys.ALARM_TRIGGER_AT)
            it.remove(PreferenceKeys.ALARM_RINGING_SINCE)
        }
    }

    private fun MutablePreferences.putAlarm(alarm: Alarm) {
        this[PreferenceKeys.ALARM_MINUTE_OF_DAY] = alarm.time.hour * 60 + alarm.time.minute
        this[PreferenceKeys.ALARM_REPEAT_DAYS] = alarm.repeatDays.mapTo(mutableSetOf()) { it.name }
        this[PreferenceKeys.ALARM_TRIGGER_AT] = alarm.triggerAt.toEpochMilli()
    }

    private fun Preferences.toAlarmState(): AlarmState {
        val minuteOfDay = this[PreferenceKeys.ALARM_MINUTE_OF_DAY] ?: return AlarmState.None
        val triggerAt = this[PreferenceKeys.ALARM_TRIGGER_AT] ?: return AlarmState.None
        val alarm = Alarm(
            time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60),
            repeatDays = this[PreferenceKeys.ALARM_REPEAT_DAYS].orEmpty()
                .mapNotNullTo(mutableSetOf()) { name -> DayOfWeek.entries.firstOrNull { it.name == name } },
            triggerAt = Instant.ofEpochMilli(triggerAt),
        )
        val ringingSince = this[PreferenceKeys.ALARM_RINGING_SINCE]
        return if (ringingSince != null) {
            AlarmState.Ringing(alarm, Instant.ofEpochMilli(ringingSince))
        } else {
            AlarmState.Scheduled(alarm)
        }
    }
}
