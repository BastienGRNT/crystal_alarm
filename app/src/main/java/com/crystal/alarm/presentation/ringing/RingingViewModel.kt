package com.crystal.alarm.presentation.ringing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crystal.alarm.domain.usecase.DismissResult
import com.crystal.alarm.domain.usecase.DismissWithCodeUseCase
import com.crystal.alarm.domain.usecase.ObserveDashboardUseCase
import com.crystal.alarm.presentation.scanner.ScanFeedback
import com.crystal.alarm.presentation.scanner.ScannedCode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RingingMode { Ring, Scan, Rescue }

data class RingingUiState(
    val mode: RingingMode = RingingMode.Ring,
    val feedback: ScanFeedback = ScanFeedback.Idle,
    /** Name of the code to scan, as a reminder of where to go. */
    val codeName: String? = null,
)

@HiltViewModel
class RingingViewModel @Inject constructor(
    private val dismissWithCode: DismissWithCodeUseCase,
    observeDashboard: ObserveDashboardUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(RingingUiState())
    val state: StateFlow<RingingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeDashboard().collect { dashboard ->
                _state.update { it.copy(codeName = dashboard.settings.activeWakeCode?.name) }
            }
        }
    }

    fun onScanRequested() = _state.update { it.copy(mode = RingingMode.Scan, feedback = ScanFeedback.Idle) }

    fun onRescueRequested() = _state.update { it.copy(mode = RingingMode.Rescue) }

    /** Back never leaves the ringing screen, it only returns to the main ringing view. */
    fun onBack() = _state.update { it.copy(mode = RingingMode.Ring, feedback = ScanFeedback.Idle) }

    fun onScanned(code: ScannedCode) {
        if (_state.value.feedback != ScanFeedback.Idle) return
        viewModelScope.launch {
            when (dismissWithCode(code.value)) {
                // The activity finishes itself when the alarm state leaves Ringing.
                DismissResult.Dismissed -> _state.update { it.copy(feedback = ScanFeedback.Success) }
                DismissResult.WrongCode -> {
                    _state.update { it.copy(feedback = ScanFeedback.Error) }
                    delay(WRONG_CODE_COOLDOWN_MS)
                    _state.update { it.copy(feedback = ScanFeedback.Idle) }
                }
                DismissResult.NotRinging -> Unit
            }
        }
    }

    private companion object {
        const val WRONG_CODE_COOLDOWN_MS = 1_500L
    }
}
