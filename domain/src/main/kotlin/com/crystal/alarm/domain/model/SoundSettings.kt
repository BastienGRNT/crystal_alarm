package com.crystal.alarm.domain.model

/**
 * How the alarm sounds.
 *
 * @property ringtoneId id of a [Ringtone].
 * @property volumePercent alarm stream volume enforced while ringing.
 * @property progressive when true, the sound rises from almost silent to [volumePercent]
 * over [RAMP_SECONDS]; otherwise it starts at full volume right away.
 * @property vibrate whether the phone also vibrates while ringing.
 */
data class SoundSettings(
    val ringtoneId: String = DEFAULT_RINGTONE_ID,
    val volumePercent: Int = DEFAULT_VOLUME,
    val progressive: Boolean = false,
    val vibrate: Boolean = true,
) {
    companion object {
        const val DEFAULT_RINGTONE_ID = "builtin:forte"
        const val DEFAULT_VOLUME = 100
        // Below this an alarm stops being an alarm.
        const val MIN_VOLUME = 20
        const val MAX_VOLUME = 100
        const val VOLUME_STEP = 10
        const val RAMP_SECONDS = 60
    }
}
