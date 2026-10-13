package com.crystal.alarm.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crystal.alarm.domain.model.Lock
import com.crystal.alarm.domain.port.TimeSource
import com.crystal.alarm.domain.usecase.CancelAlarmUseCase
import com.crystal.alarm.domain.usecase.Dashboard
import com.crystal.alarm.domain.usecase.ObserveDashboardUseCase
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
    ) : HomeUiState {
        val activeLock: Lock? get() = dashboard.lock?.takeIf { it.isActive(now) }
        val hasWakeCode: Boolean get() = dashboard.settings.activeWakeCode != null
    }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeDashboard: ObserveDashboardUseCase,
    private val cancelAlarm: CancelAlarmUseCase,
    private val timeSource: TimeSource,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(observeDashboard(), tickerFlow()) { dashboard, _ ->
        HomeUiState.Ready(dashboard, timeSource.now(), timeSource.zone())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    fun onDelete() {
        viewModelScope.launch { cancelAlarm() }
    }
}
