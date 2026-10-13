package com.crystal.alarm.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crystal.alarm.domain.model.AlarmState
import com.crystal.alarm.domain.model.Lock
import com.crystal.alarm.domain.port.TimeSource
import com.crystal.alarm.domain.usecase.CancelAlarmUseCase
import com.crystal.alarm.domain.usecase.Dashboard
import com.crystal.alarm.domain.usecase.EnableAlarmUseCase
import com.crystal.alarm.domain.usecase.ObserveDashboardUseCase
import com.crystal.alarm.domain.usecase.ObserveWakeUpStatsUseCase
import com.crystal.alarm.domain.usecase.WakeUpStats
import com.crystal.alarm.presentation.components.tickerFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Ready(
        val dashboard: Dashboard,
        val now: Instant,
        val zone: ZoneId,
        val wakeUps: WakeUpStats,
    ) : HomeUiState {
        val activeLock: Lock? get() = dashboard.lock?.takeIf { it.isActive(now) }
        val hasWakeCode: Boolean get() = dashboard.settings.activeWakeCode != null
        val isRinging: Boolean get() = dashboard.alarm is AlarmState.Ringing

        /** Same conditions as the "Modifier" button: creating or moving an alarm is allowed. */
        val canEdit: Boolean get() = !isRinging && hasWakeCode && activeLock == null
    }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeDashboard: ObserveDashboardUseCase,
    observeWakeUpStats: ObserveWakeUpStatsUseCase,
    private val cancelAlarm: CancelAlarmUseCase,
    private val enableAlarm: EnableAlarmUseCase,
    private val timeSource: TimeSource,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(observeDashboard(), observeWakeUpStats(), tickerFlow()) { dashboard, wakeUps, _ ->
        HomeUiState.Ready(dashboard, timeSource.now(), timeSource.zone(), wakeUps)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    /** Enabling follows the creation rules (lock, wake code); the switch is disabled when they say no. */
    fun onToggleAlarm(enabled: Boolean) {
        viewModelScope.launch { if (enabled) enableAlarm() else cancelAlarm() }
    }
}
