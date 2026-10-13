package com.crystal.alarm.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crystal.alarm.domain.port.TimeSource
import com.crystal.alarm.domain.usecase.ObserveWakeUpStatsUseCase
import com.crystal.alarm.domain.usecase.WakeUpStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId
import javax.inject.Inject

data class HistoryUiState(
    val stats: WakeUpStats? = null,
    val zone: ZoneId = ZoneId.systemDefault(),
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    observeWakeUpStats: ObserveWakeUpStatsUseCase,
    timeSource: TimeSource,
) : ViewModel() {

    val state: StateFlow<HistoryUiState> = observeWakeUpStats()
        .map { HistoryUiState(it, timeSource.zone()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())
}
