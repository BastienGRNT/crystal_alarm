package com.crystal.alarm.domain.port

/** Plays a short preview of a ringtone at a given volume, from the settings screen. */
interface RingtonePreviewer {
    fun play(ringtoneId: String, volumePercent: Int)

    fun stop()
}
