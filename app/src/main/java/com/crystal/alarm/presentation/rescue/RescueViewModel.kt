package com.crystal.alarm.presentation.rescue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crystal.alarm.domain.rescue.RescueAnswer
import com.crystal.alarm.domain.rescue.RescueChallenge
import com.crystal.alarm.domain.rescue.RescueProgress
import com.crystal.alarm.domain.usecase.DismissWithRescueUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RescueFeedback {
    data object None : RescueFeedback
    data object Correct : RescueFeedback
    data class Wrong(val expected: Int) : RescueFeedback
}

data class RescueUiState(
    val progress: RescueProgress,
    val input: String = "",
    val feedback: RescueFeedback = RescueFeedback.None,
    /** Incremented on each mistake, drives the shake animation. */
    val mistakes: Int = 0,
    val completed: Boolean = false,
)

@HiltViewModel
class RescueViewModel @Inject constructor(
    private val challenge: RescueChallenge,
    private val dismissWithRescue: DismissWithRescueUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(RescueUiState(progress = challenge.start()))
    val state: StateFlow<RescueUiState> = _state.asStateFlow()

    fun onDigit(digit: Int) = _state.update {
        if (it.completed || it.input.length >= MAX_DIGITS) it else it.copy(input = it.input + digit)
    }

    fun onBackspace() = _state.update { it.copy(input = it.input.dropLast(1)) }

    fun onSubmit() {
        val current = _state.value
        val value = current.input.toIntOrNull() ?: return
        if (current.completed) return
        when (val answer = challenge.answer(current.progress, value)) {
            is RescueAnswer.Correct -> _state.value = current.copy(
                progress = answer.progress,
                input = "",
                feedback = RescueFeedback.Correct,
            )
            is RescueAnswer.Wrong -> _state.value = current.copy(
                progress = answer.progress,
                input = "",
                feedback = RescueFeedback.Wrong(answer.expected),
                mistakes = current.mistakes + 1,
            )
            is RescueAnswer.Completed -> {
                _state.value = current.copy(progress = answer.progress, input = "", completed = true)
                viewModelScope.launch { dismissWithRescue(answer.progress) }
            }
        }
    }

    private companion object {
        const val MAX_DIGITS = 4 // 99 × 99 = 9801
    }
}
