package com.crystal.alarm.domain.usecase

import com.crystal.alarm.domain.model.AlarmState
import com.crystal.alarm.domain.port.AlarmScheduler
import com.crystal.alarm.domain.port.TimeSource
import com.crystal.alarm.domain.repository.AlarmRepository
import java.time.Duration
import javax.inject.Inject

/**
 * Re-arms the OS alarm after a reboot, an app update or a clock/time zone change.
 *
 * Anti-cheat: rebooting while ringing rings again immediately, and an alarm missed
 * while the phone was off rings as soon as it's back (within [MISSED_GRACE]).
 */
class RestoreAlarmUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val scheduler: AlarmScheduler,
    private val timeSource: TimeSource,
    private val computeNextTrigger: ComputeNextTriggerUseCase,
) {
    suspend operator fun invoke() {
        val now = timeSource.now()
        when (val state = alarmRepository.current()) {
            is AlarmState.None -> Unit
            is AlarmState.Ringing -> scheduler.fireNow()
            is AlarmState.Scheduled -> {
                val alarm = state.alarm
                val missed = !alarm.triggerAt.isAfter(now)
                when {
                    missed && Duration.between(alarm.triggerAt, now) <= MISSED_GRACE -> scheduler.fireNow()
                    missed && !alarm.isRepeating -> {
                        alarmRepository.clear()
                        scheduler.cancel()
                    }
                    else -> {
                        // Wall-clock semantics: 07:00 stays 07:00 after a time zone change.
                        val next = alarm.copy(
                            triggerAt = computeNextTrigger(alarm.time, alarm.repeatDays, now, timeSource.zone()),
                        )
                        alarmRepository.schedule(next)
                        scheduler.schedule(next)
                    }
                }
            }
        }
    }

    companion object {
        val MISSED_GRACE: Duration = Duration.ofHours(3)
    }
}
