package com.crystal.alarm.platform.alarm

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.crystal.alarm.domain.model.AlarmState
import com.crystal.alarm.domain.repository.AlarmRepository
import com.crystal.alarm.domain.repository.SettingsRepository
import com.crystal.alarm.domain.usecase.StartRingingUseCase
import com.crystal.alarm.platform.sound.AlarmSoundPlayer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Keeps the alarm ringing until the domain state leaves [AlarmState.Ringing].
 * Dismissal never talks to this service: it just changes the state, and the service reacts.
 */
@AndroidEntryPoint
class AlarmRingingService : LifecycleService() {

    @Inject lateinit var startRinging: StartRingingUseCase
    @Inject lateinit var alarmRepository: AlarmRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var soundPlayer: AlarmSoundPlayer

    private var ringingJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        ServiceCompat.startForeground(
            this,
            AlarmNotifications.RINGING_NOTIFICATION_ID,
            AlarmNotifications.ringing(this),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
        if (ringingJob == null) {
            ringingJob = lifecycleScope.launch {
                if (startRinging() != null) {
                    soundPlayer.start(lifecycleScope, settingsRepository.current().sound)
                    val keeper = launch { RingingScreenKeeper(this@AlarmRingingService).keepInFront() }
                    alarmRepository.state.first { it !is AlarmState.Ringing }
                    keeper.cancel()
                }
                shutdown()
            }
        }
        // If the process gets killed while ringing, the system restarts us and we ring again.
        return START_STICKY
    }

    override fun onDestroy() {
        soundPlayer.stop()
        super.onDestroy()
    }

    private fun shutdown() {
        soundPlayer.stop()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, AlarmRingingService::class.java))
        }
    }
}
