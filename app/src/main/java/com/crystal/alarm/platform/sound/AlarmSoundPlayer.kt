package com.crystal.alarm.platform.sound

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Build
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.getSystemService
import com.crystal.alarm.data.ringtone.DataStoreRingtoneRepository
import com.crystal.alarm.domain.model.SoundSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Plays the chosen ringtone in a loop at the chosen alarm volume, re-asserted continuously
 * (turning the volume down doesn't help), optionally rising progressively, optionally vibrating.
 * Restores the user's alarm volume on [stop].
 */
class AlarmSoundPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ringtones: DataStoreRingtoneRepository,
) {
    private val audioManager: AudioManager? = context.getSystemService()
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService<VibratorManager>()?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService()
        }

    private var player: MediaPlayer? = null
    private var originalVolume: Int? = null
    private var loopJob: Job? = null

    fun start(scope: CoroutineScope, sound: SoundSettings) {
        if (loopJob != null) return
        val audio = audioManager
        originalVolume = audio?.getStreamVolume(AudioManager.STREAM_ALARM)
        val targetVolume = audio?.alarmStreamVolume(sound.volumePercent)
        targetVolume?.let { audio.setAlarmStreamVolume(it) }

        player = context.startFirstPlayable(ringtones.playbackCandidates(sound.ringtoneId), looping = true, wakeLock = true)
        if (sound.progressive) player?.setVolume(MIN_GAIN, MIN_GAIN)
        val fallbackTone = if (player == null) ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME) else null

        val startedAt = SystemClock.elapsedRealtime()
        var lastBeep = 0L
        loopJob = scope.launch {
            while (isActive) {
                targetVolume?.let { audio.setAlarmStreamVolume(it) }
                val elapsed = SystemClock.elapsedRealtime() - startedAt
                if (sound.progressive) player?.let { applyRamp(it, elapsed) }
                if (fallbackTone != null && elapsed - lastBeep >= 1_000) {
                    fallbackTone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 800)
                    lastBeep = elapsed
                }
                delay(TICK_MS)
            }
        }.also { job -> job.invokeOnCompletion { fallbackTone?.release() } }

        if (sound.vibrate) vibrate()
    }

    fun stop() {
        loopJob?.cancel()
        loopJob = null
        player?.runCatching {
            stop()
            release()
        }
        player = null
        vibrator?.cancel()
        originalVolume?.let { audioManager?.setAlarmStreamVolume(it) }
        originalVolume = null
    }

    /** Quadratic curve: loudness feels linear to the ear. */
    private fun applyRamp(player: MediaPlayer, elapsedMs: Long) {
        val progress = (elapsedMs / (SoundSettings.RAMP_SECONDS * 1_000f)).coerceIn(0f, 1f)
        val gain = MIN_GAIN + (1f - MIN_GAIN) * progress * progress
        runCatching { player.setVolume(gain, gain) }
    }

    private fun vibrate() {
        val vibrator = vibrator?.takeIf { it.hasVibrator() } ?: return
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 800, 500), 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, AlarmAudioAttributes)
        }
    }

    private companion object {
        const val MIN_GAIN = 0.03f
        const val TICK_MS = 250L
    }
}
