package com.crystal.alarm.domain.model

data class Settings(
    val wakeCodes: List<WakeCode> = emptyList(),
    val activeWakeCodeId: String? = null,
    val lockMinutes: Int = DEFAULT_LOCK_MINUTES,
    val sound: SoundSettings = SoundSettings(),
) {
    /** The code to scan in the morning. Null only when no code is saved. */
    val activeWakeCode: WakeCode?
        get() = wakeCodes.firstOrNull { it.id == activeWakeCodeId }

    companion object {
        const val DEFAULT_LOCK_MINUTES = 30
        const val MIN_LOCK_MINUTES = 5
        const val MAX_LOCK_MINUTES = 180
        const val LOCK_STEP_MINUTES = 5
    }
}
