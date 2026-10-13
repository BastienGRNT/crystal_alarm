package com.crystal.alarm.platform.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import kotlin.math.ceil

internal val AlarmAudioAttributes: AudioAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_ALARM)
    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    .build()

/** Alarm stream index matching [volumePercent], never 0. */
internal fun AudioManager.alarmStreamVolume(volumePercent: Int): Int {
    val max = getStreamMaxVolume(AudioManager.STREAM_ALARM)
    return ceil(max * volumePercent / 100.0).toInt().coerceIn(1, max)
}

internal fun AudioManager.setAlarmStreamVolume(index: Int) {
    if (getStreamVolume(AudioManager.STREAM_ALARM) != index) {
        runCatching { setStreamVolume(AudioManager.STREAM_ALARM, index, 0) }
    }
}

/** Starts the first candidate that actually plays. */
internal fun Context.startFirstPlayable(candidates: List<Uri>, looping: Boolean, wakeLock: Boolean): MediaPlayer? {
    for (uri in candidates) {
        val player = MediaPlayer()
        val started = runCatching {
            player.setAudioAttributes(AlarmAudioAttributes)
            player.setDataSource(this, uri)
            player.isLooping = looping
            if (wakeLock) player.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
            player.prepare()
            player.start()
        }.isSuccess
        if (started) return player
        player.release()
    }
    return null
}
