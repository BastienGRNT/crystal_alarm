package com.crystal.alarm.domain.usecase

import com.crystal.alarm.domain.model.AlarmState
import com.crystal.alarm.domain.repository.AlarmRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Turns the last alarm back on, at the same time. Same rules as creating an alarm (lock, wake code,
 * ringing): it goes through [SetAlarmUseCase]. Returns null when there is nothing to enable.
 */
class EnableAlarmUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val setAlarm: SetAlarmUseCase,
) {
    suspend operator fun invoke(): SetAlarmResult? {
        when (val state = alarmRepository.current()) {
            is AlarmState.Scheduled -> return SetAlarmResult.Scheduled(state.alarm)
            is AlarmState.Ringing -> return SetAlarmResult.AlarmRinging
            AlarmState.None -> Unit
        }
        val time = alarmRepository.lastTime.first() ?: return null
        return setAlarm(time)
    }
}
