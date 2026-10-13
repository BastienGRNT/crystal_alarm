package com.crystal.alarm.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crystal.alarm.domain.model.Lock
import com.crystal.alarm.domain.model.Ringtone
import com.crystal.alarm.domain.model.Settings
import com.crystal.alarm.domain.port.TimeSource
import com.crystal.alarm.domain.usecase.DeleteWakeCodeResult
import com.crystal.alarm.domain.usecase.DeleteWakeCodeUseCase
import com.crystal.alarm.domain.usecase.ObserveDashboardUseCase
import com.crystal.alarm.domain.usecase.ObserveRingtonesUseCase
import com.crystal.alarm.domain.usecase.RenameWakeCodeUseCase
import com.crystal.alarm.domain.usecase.SelectWakeCodeUseCase
import com.crystal.alarm.domain.usecase.UpdateLockMinutesUseCase
import com.crystal.alarm.domain.usecase.UpdateSleepGoalUseCase
import com.crystal.alarm.presentation.components.tickerFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

enum class SettingsMessage { LastCodeWithAlarm }

data class SettingsUiState(
    val settings: Settings? = null,
    val ringtone: Ringtone? = null,
    val activeLock: Lock? = null,
    val now: Instant = Instant.EPOCH,
    val message: SettingsMessage? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeDashboard: ObserveDashboardUseCase,
    observeRingtones: ObserveRingtonesUseCase,
    private val updateLockMinutes: UpdateLockMinutesUseCase,
    private val updateSleepGoal: UpdateSleepGoalUseCase,
    private val selectWakeCode: SelectWakeCodeUseCase,
    private val renameWakeCode: RenameWakeCodeUseCase,
    private val deleteWakeCode: DeleteWakeCodeUseCase,
    private val timeSource: TimeSource,
) : ViewModel() {

    private val message = MutableStateFlow<SettingsMessage?>(null)

    val state: StateFlow<SettingsUiState> =
        combine(observeDashboard(), observeRingtones(), message, tickerFlow()) { dashboard, ringtones, message, _ ->
            val now = timeSource.now()
            SettingsUiState(
                settings = dashboard.settings,
                ringtone = ringtones.firstOrNull { it.id == dashboard.settings.sound.ringtoneId },
                activeLock = dashboard.lock?.takeIf { it.isActive(now) },
                now = now,
                message = message,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun onLockMinutesChange(minutes: Int) {
        viewModelScope.launch { updateLockMinutes(minutes) }
    }

    fun onSleepGoalChange(minutes: Int) {
        viewModelScope.launch { updateSleepGoal(minutes) }
    }

    fun onSelectCode(id: String) {
        viewModelScope.launch { selectWakeCode(id) }
    }

    fun onRenameCode(id: String, name: String) {
        viewModelScope.launch { renameWakeCode(id, name) }
    }

    fun onDeleteCode(id: String) {
        viewModelScope.launch {
            if (deleteWakeCode(id) == DeleteWakeCodeResult.LastCodeWithAlarm) {
                message.value = SettingsMessage.LastCodeWithAlarm
            }
        }
    }

    fun onMessageShown() {
        message.value = null
    }
}
