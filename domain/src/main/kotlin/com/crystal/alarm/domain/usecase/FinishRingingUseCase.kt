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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.time.Duration
import javax.inject.Inject

/**
 * Stops a ringing alarm, records how long it took and starts the post-dismissal lock.
 * The alarm then turns off (its time is kept for one-tap re-enabling).
 *
 * Only meant to be called by [DismissWithCodeUseCase] and [DismissWithRescueUseCase],
 * which own the "proof of wakefulness" rules.
 *
 * All or nothing: it runs to completion even if the caller gets cancelled. The ringing
 * screen closes the instant the alarm turns off, which cancels its ViewModel scope; without
 * this, the lock and the history entry written afterwards were silently lost.
 */
class FinishRingingUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val lockRepository: LockRepository,
    private val settingsRepository: SettingsRepository,
    private val historyRepository: WakeUpHistoryRepository,
    private val scheduler: AlarmScheduler,
    private val timeSource: TimeSource,
) {
    suspend operator fun invoke(method: DismissMethod): Boolean = withContext(NonCancellable) {
        val ringing = alarmRepository.current() as? AlarmState.Ringing ?: return@withContext false
        val now = timeSource.now()
        val alarm = ringing.alarm

        // Lock first: the moment the alarm shows as off, re-enabling it must already be refused.
        val lockMinutes = settingsRepository.current().lockMinutes.toLong()
        lockRepository.set(Lock(since = now, until = now.plus(Duration.ofMinutes(lockMinutes))))

        // One-shot by design: once stopped, the alarm is off until re-enabled by hand.
        alarmRepository.clear()
        scheduler.cancel()

        historyRepository.add(WakeUpRecord(alarm.time, rangAt = ringing.since, stoppedAt = now, method = method))
        true
    }
}
