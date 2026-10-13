package com.crystal.alarm.presentation.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crystal.alarm.domain.model.AlarmState
import com.crystal.alarm.domain.port.TimeSource
import com.crystal.alarm.domain.usecase.ComputeNextTriggerUseCase
import com.crystal.alarm.domain.usecase.ObserveDashboardUseCase
import com.crystal.alarm.domain.usecase.SetAlarmResult
import com.crystal.alarm.domain.usecase.SetAlarmUseCase
import com.crystal.alarm.presentation.components.tickerFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalTime
import javax.inject.Inject

data class EditorForm(
    val hour: Int,
    val minute: Int,
    val isNew: Boolean,
)

enum class EditorError { Locked, MissingWakeCode, Ringing }

data class EditorUiState(
    val form: EditorForm? = null,
    val ringsIn: Duration? = null,
    val error: EditorError? = null,
    val saving: Boolean = false,
)

sealed interface EditorEvent {
    data object Saved : EditorEvent
}

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val observeDashboard: ObserveDashboardUseCase,
    private val setAlarm: SetAlarmUseCase,
    private val computeNextTrigger: ComputeNextTriggerUseCase,
    private val timeSource: TimeSource,
) : ViewModel() {

    private val form = MutableStateFlow<EditorForm?>(null)
    private val error = MutableStateFlow<EditorError?>(null)
    private val saving = MutableStateFlow(false)

    private val _events = Channel<EditorEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val state: StateFlow<EditorUiState> =
        combine(form, error, saving, tickerFlow()) { form, error, saving, _ ->
            EditorUiState(
                form = form,
                ringsIn = form?.let {
                    val now = timeSource.now()
                    val trigger = computeNextTrigger(LocalTime.of(it.hour, it.minute), now, timeSource.zone())
                    Duration.between(now, trigger)
                },
                error = error,
                saving = saving,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditorUiState())

    init {
        viewModelScope.launch {
            // Defaults to the current alarm, or the last one even if it's disabled.
            val dashboard = observeDashboard().first()
            val existing = (dashboard.alarm as? AlarmState.Scheduled)?.alarm?.time ?: dashboard.lastTime
            form.value = EditorForm(
                hour = existing?.hour ?: DEFAULT_HOUR,
                minute = existing?.minute ?: 0,
                isNew = existing == null,
            )
        }
    }

    fun onHourChange(hour: Int) = form.update { it?.copy(hour = hour) }

    fun onMinuteChange(minute: Int) = form.update { it?.copy(minute = minute) }

    fun onSave() {
        val current = form.value ?: return
        if (saving.value) return
        viewModelScope.launch {
            saving.value = true
            error.value = when (setAlarm(LocalTime.of(current.hour, current.minute))) {
                is SetAlarmResult.Scheduled -> null.also { _events.send(EditorEvent.Saved) }
                is SetAlarmResult.Locked -> EditorError.Locked
                SetAlarmResult.MissingWakeCode -> EditorError.MissingWakeCode
                SetAlarmResult.AlarmRinging -> EditorError.Ringing
            }
            saving.value = false
        }
    }

    private companion object {
        const val DEFAULT_HOUR = 7
    }
}
