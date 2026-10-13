package com.crystal.alarm.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crystal.alarm.domain.model.WakeCode
import com.crystal.alarm.domain.usecase.AddWakeCodeResult
import com.crystal.alarm.domain.usecase.AddWakeCodeUseCase
import com.crystal.alarm.domain.usecase.SelectWakeCodeUseCase
import com.crystal.alarm.presentation.scanner.ScannedCode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val detected: ScannedCode? = null,
    val name: String = "",
    /** Set when the scanned code is already saved under another name. */
    val alreadySaved: WakeCode? = null,
    val saving: Boolean = false,
) {
    val canSave: Boolean get() = detected != null && name.isNotBlank() && !saving
}

@HiltViewModel
class RegisterCodeViewModel @Inject constructor(
    private val addWakeCode: AddWakeCodeUseCase,
    private val selectWakeCode: SelectWakeCodeUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved = _saved.receiveAsFlow()

    fun onScanned(code: ScannedCode) = _state.update { if (it.detected == null) it.copy(detected = code) else it }

    fun onNameChange(name: String) = _state.update { it.copy(name = name.take(WakeCode.MAX_NAME_LENGTH)) }

    fun onRetry() = _state.update { RegisterUiState() }

    fun onSave() {
        val current = _state.value
        val code = current.detected ?: return
        if (!current.canSave) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            when (val result = addWakeCode(current.name, code.value, code.format)) {
                is AddWakeCodeResult.Added -> _saved.send(Unit)
                is AddWakeCodeResult.AlreadySaved -> _state.update { it.copy(alreadySaved = result.existing) }
                AddWakeCodeResult.InvalidName, AddWakeCodeResult.AlarmRinging -> Unit
            }
            _state.update { it.copy(saving = false) }
        }
    }

    /** The code is already known: just make it the active one. */
    fun onUseExisting() {
        val existing = _state.value.alreadySaved ?: return
        viewModelScope.launch {
            if (selectWakeCode(existing.id)) _saved.send(Unit)
        }
    }
}
