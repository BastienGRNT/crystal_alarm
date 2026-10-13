package com.crystal.alarm.domain.usecase

import com.crystal.alarm.domain.model.AlarmState
import com.crystal.alarm.domain.model.DismissMethod
import com.crystal.alarm.domain.model.Lock
import com.crystal.alarm.domain.model.WakeUpRecord
import com.crystal.alarm.domain.port.AlarmScheduler
import com.crystal.alarm.domain.port.TimeSource
import com.crystal.alarm.domain.repository.AlarmRepository
import com.crystal.alarm.domain.repository.LockRepository
import com.crystal.alarm.domain.repository.SettingsRepository
import com.crystal.alarm.domain.repository.WakeUpHistoryRepository
import java.time.Duration
import javax.inject.Inject

/**
 * Stops a ringing alarm, records how long it took and starts the post-dismissal lock.
 * The alarm then turns off (its time is kept for one-tap re-enabling).
 *
 * Only meant to be called by [DismissWithCodeUseCase] and [DismissWithRescueUseCase],
 * which own the "proof of wakefulness" rules.
 */
class FinishRingingUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val lockRepository: LockRepository,
    private val settingsRepository: SettingsRepository,
    private val historyRepository: WakeUpHistoryRepository,
    private val scheduler: AlarmScheduler,
    private val timeSource: TimeSource,
) {
    suspend operator fun invoke(method: DismissMethod): Boolean {
        val ringing = alarmRepository.current() as? AlarmState.Ringing ?: return false
        val now = timeSource.now()
        val alarm = ringing.alarm

        // One-shot by design: once stopped, the alarm is off until re-enabled by hand.
        alarmRepository.clear()
        scheduler.cancel()

        historyRepository.add(WakeUpRecord(alarm.time, rangAt = ringing.since, stoppedAt = now, method = method))

        val lockMinutes = settingsRepository.current().lockMinutes.toLong()
        lockRepository.set(Lock(since = now, until = now.plus(Duration.ofMinutes(lockMinutes))))
        return true
    }
}
