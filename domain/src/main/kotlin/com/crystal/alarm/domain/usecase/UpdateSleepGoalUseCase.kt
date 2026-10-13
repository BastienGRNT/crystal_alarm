package com.crystal.alarm.domain.usecase

import com.crystal.alarm.domain.model.Settings
import com.crystal.alarm.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Changes the sleep goal used by the widget (bedtime countdown and colors).
 * Not tied to the lock: it never lets the user move or snooze an alarm.
 */
class UpdateSleepGoalUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(minutes: Int) {
        val step = Settings.SLEEP_GOAL_STEP_MINUTES
        val snapped = ((minutes + step / 2) / step * step)
            .coerceIn(Settings.MIN_SLEEP_GOAL_MINUTES, Settings.MAX_SLEEP_GOAL_MINUTES)
        settingsRepository.setSleepGoalMinutes(snapped)
    }
}
