package com.crystal.alarm.domain.usecase

import com.crystal.alarm.domain.model.AlarmState
import com.crystal.alarm.domain.port.AlarmScheduler
import com.crystal.alarm.domain.port.TimeSource
import com.crystal.alarm.domain.repository.AlarmRepository
import java.time.Duration
import javax.inject.Inject

/**
 * Called when the OS fires the alarm. Returns the ringing state, or null when there is
 * nothing to ring (alarm deleted meanwhile, or a stale early fire that gets rescheduled).
 */
class StartRingingUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val scheduler: AlarmScheduler,
    private val timeSource: TimeSource,
) {
    suspend operator fun invoke(): AlarmState.Ringing? {
        val now = timeSource.now()
        return when (val state = alarmRepository.current()) {
            is AlarmState.Ringing -> state
            is AlarmState.None -> null
            is AlarmState.Scheduled -> {
                val alarm = state.alarm
                if (alarm.triggerAt.isAfter(now.plus(EARLY_FIRE_TOLERANCE))) {
                    scheduler.schedule(alarm)
                    null
                } else {
                    alarmRepository.markRinging(alarm, now)
                    AlarmState.Ringing(alarm, now)
                }
            }
        }
    }

    private companion object {
        val EARLY_FIRE_TOLERANCE: Duration = Duration.ofMinutes(1)
    }
}
