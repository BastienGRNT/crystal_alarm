package com.crystal.alarm.domain.usecase

import com.crystal.alarm.domain.model.Alarm
import com.crystal.alarm.domain.model.AlarmState
import com.crystal.alarm.domain.model.Lock
import com.crystal.alarm.domain.port.AlarmScheduler
import com.crystal.alarm.domain.port.TimeSource
import com.crystal.alarm.domain.repository.AlarmRepository
import com.crystal.alarm.domain.repository.LockRepository
import com.crystal.alarm.domain.repository.SettingsRepository
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

sealed interface SetAlarmResult {
    data class Scheduled(val alarm: Alarm) : SetAlarmResult
    data object MissingWakeCode : SetAlarmResult
    data class Locked(val lock: Lock) : SetAlarmResult
    data object AlarmRinging : SetAlarmResult
}

/**
 * Creates or replaces the single alarm.
 * Refused while ringing, while the post-dismissal lock is active, or without a wake code
 * (an alarm that can't be stopped by scanning would be pointless).
 */
class SetAlarmUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val lockRepository: LockRepository,
    private val settingsRepository: SettingsRepository,
    private val scheduler: AlarmScheduler,
    private val timeSource: TimeSource,
    private val computeNextTrigger: ComputeNextTriggerUseCase,
) {
    suspend operator fun invoke(time: LocalTime, repeatDays: Set<DayOfWeek>): SetAlarmResult {
        val now = timeSource.now()
        if (alarmRepository.current() is AlarmState.Ringing) return SetAlarmResult.AlarmRinging
        if (settingsRepository.current().activeWakeCode == null) return SetAlarmResult.MissingWakeCode
        lockRepository.current()
            ?.takeIf { it.isActive(now) }
            ?.let { return SetAlarmResult.Locked(it) }

        val alarm = Alarm(
            time = time,
            repeatDays = repeatDays,
            triggerAt = computeNextTrigger(time, repeatDays, now, timeSource.zone()),
        )
        alarmRepository.schedule(alarm)
        scheduler.schedule(alarm)
        return SetAlarmResult.Scheduled(alarm)
    }
}
