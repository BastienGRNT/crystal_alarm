package com.crystal.alarm.platform.sound

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import androidx.core.content.getSystemService
import com.crystal.alarm.data.ringtone.DataStoreRingtoneRepository
import com.crystal.alarm.domain.port.RingtonePreviewer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Previews a ringtone exactly as it will ring (alarm stream, chosen volume), for a few seconds,
 * then puts the user's alarm volume back.
 */
@Singleton
class AndroidRingtonePreviewer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ringtones: DataStoreRingtoneRepository,
) : RingtonePreviewer {

    private val audioManager: AudioManager? = context.getSystemService()
    private val handler = Handler(Looper.getMainLooper())
    private val autoStop = Runnable { stop() }
    private var player: MediaPlayer? = null
    private var originalVolume: Int? = null

    override fun play(ringtoneId: String, volumePercent: Int) {
        stop()
        audioManager?.let { audio ->
            originalVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM)
            audio.setAlarmStreamVolume(audio.alarmStreamVolume(volumePercent))
        }
        // Preview only the requested sound: no silent fallback to another one.
        player = context.startFirstPlayable(ringtones.playbackCandidates(ringtoneId).take(1), looping = true, wakeLock = false)
        handler.postDelayed(autoStop, PREVIEW_MS)
    }

    override fun stop() {
        handler.removeCallbacks(autoStop)
        player?.runCatching {
            stop()
            release()
        }
        player = null
        originalVolume?.let { audioManager?.setAlarmStreamVolume(it) }
        originalVolume = null
    }

    private companion object {
        const val PREVIEW_MS = 6_000L
    }
}
