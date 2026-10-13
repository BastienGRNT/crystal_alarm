package com.crystal.alarm.domain.usecase

import com.crystal.alarm.domain.model.AlarmState
import com.crystal.alarm.domain.port.AlarmScheduler
import com.crystal.alarm.domain.repository.AlarmRepository
import javax.inject.Inject

/** Deletes the scheduled alarm. A ringing alarm can never be cancelled, only dismissed. */
class CancelAlarmUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val scheduler: AlarmScheduler,
) {
    suspend operator fun invoke(): Boolean {
        if (alarmRepository.current() !is AlarmState.Scheduled) return false
        scheduler.cancel()
        alarmRepository.clear()
        return true
    }
}
