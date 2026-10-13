package com.crystal.alarm.domain.usecase

import com.crystal.alarm.domain.model.AlarmState
import com.crystal.alarm.domain.model.Lock
import com.crystal.alarm.domain.port.AlarmScheduler
import com.crystal.alarm.domain.port.TimeSource
import com.crystal.alarm.domain.repository.AlarmRepository
import com.crystal.alarm.domain.repository.LockRepository
import com.crystal.alarm.domain.repository.SettingsRepository
import java.time.Duration
import javax.inject.Inject

/**
 * Stops a ringing alarm and starts the post-dismissal lock.
 * Repeating alarms move to their next occurrence, one-shot alarms disappear.
 *
 * Only meant to be called by [DismissWithCodeUseCase] and [DismissWithRescueUseCase],
 * which own the "proof of wakefulness" rules.
 */
class FinishRingingUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val lockRepository: LockRepository,
    private val settingsRepository: SettingsRepository,
    private val scheduler: AlarmScheduler,
    private val timeSource: TimeSource,
    private val computeNextTrigger: ComputeNextTriggerUseCase,
) {
    suspend operator fun invoke(): Boolean {
        val ringing = alarmRepository.current() as? AlarmState.Ringing ?: return false
        val now = timeSource.now()
        val alarm = ringing.alarm

        if (alarm.isRepeating) {
            val next = alarm.copy(
                triggerAt = computeNextTrigger(alarm.time, alarm.repeatDays, now, timeSource.zone()),
            )
            alarmRepository.schedule(next)
            scheduler.schedule(next)
        } else {
            alarmRepository.clear()
            scheduler.cancel()
        }

        val lockMinutes = settingsRepository.current().lockMinutes.toLong()
        lockRepository.set(Lock(since = now, until = now.plus(Duration.ofMinutes(lockMinutes))))
        return true
    }
}
