package com.crystal.alarm.domain.model

import java.time.Duration

data class Settings(
    val wakeCodes: List<WakeCode> = emptyList(),
    val activeWakeCodeId: String? = null,
    val lockMinutes: Int = DEFAULT_LOCK_MINUTES,
    val sound: SoundSettings = SoundSettings(),
    /** Minimum night the user aims for; below it the widget turns red. */
    val sleepGoalMinutes: Int = DEFAULT_SLEEP_GOAL_MINUTES,
) {
    /** The code to scan in the morning. Null only when no code is saved. */
    val activeWakeCode: WakeCode?
        get() = wakeCodes.firstOrNull { it.id == activeWakeCodeId }

    val sleepGoal: Duration
        get() = Duration.ofMinutes(sleepGoalMinutes.toLong())

    companion object {
        const val DEFAULT_LOCK_MINUTES = 30
        const val MIN_LOCK_MINUTES = 5
        const val MAX_LOCK_MINUTES = 180
        const val LOCK_STEP_MINUTES = 5

        const val DEFAULT_SLEEP_GOAL_MINUTES = 7 * 60 + 30
        const val MIN_SLEEP_GOAL_MINUTES = 5 * 60
        const val MAX_SLEEP_GOAL_MINUTES = 10 * 60
        const val SLEEP_GOAL_STEP_MINUTES = 15
    }
}
