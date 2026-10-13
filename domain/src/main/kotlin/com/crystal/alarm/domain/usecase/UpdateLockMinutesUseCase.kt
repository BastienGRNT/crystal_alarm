package com.crystal.alarm.domain.usecase

import com.crystal.alarm.domain.model.Settings
import com.crystal.alarm.domain.port.TimeSource
import com.crystal.alarm.domain.repository.LockRepository
import com.crystal.alarm.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Changes the post-dismissal lock duration. Refused while a lock is active,
 * otherwise lowering it would be a way around the lock.
 */
class UpdateLockMinutesUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val lockRepository: LockRepository,
    private val timeSource: TimeSource,
) {
    suspend operator fun invoke(minutes: Int): Boolean {
        if (lockRepository.current()?.isActive(timeSource.now()) == true) return false
        val step = Settings.LOCK_STEP_MINUTES
        val snapped = ((minutes + step / 2) / step * step)
            .coerceIn(Settings.MIN_LOCK_MINUTES, Settings.MAX_LOCK_MINUTES)
        settingsRepository.setLockMinutes(snapped)
        return true
    }
}
