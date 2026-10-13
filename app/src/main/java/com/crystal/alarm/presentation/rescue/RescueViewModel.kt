package com.crystal.alarm.presentation.rescue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crystal.alarm.domain.rescue.RescueAnswer
import com.crystal.alarm.domain.rescue.RescueChallenge
import com.crystal.alarm.domain.rescue.RescueProgress
import com.crystal.alarm.domain.rescue.RescueStep
import com.crystal.alarm.domain.rescue.TextCheck
import com.crystal.alarm.domain.usecase.DismissWithRescueUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RescueUiState(
    val progress: RescueProgress,
    val textInput: String = "",
    val textCheck: TextCheck = TextCheck(matched = 0, hasError = false, isComplete = false),
    val calcInput: String = "",
    /** Expected result of the last wrong calculation, shown once. */
    val wrongExpected: Int? = null,
    /** Incremented on each wrong result, drives the shake animation. */
    val mistakes: Int = 0,
) {
    val completed: Boolean get() = progress.isComplete
}

@HiltViewModel
class RescueViewModel @Inject constructor(
    private val challenge: RescueChallenge,
    private val dismissWithRescue: DismissWithRescueUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(RescueUiState(progress = challenge.start()))
    val state: StateFlow<RescueUiState> = _state.asStateFlow()

    /** The calculation unlocks by itself as soon as the text is fully copied. */
    fun onTextChange(value: String) = _state.update {
        if (it.progress.step != RescueStep.CopyText) return@update it
        it.copy(
            textInput = value,
            textCheck = challenge.checkText(it.progress, value),
            progress = challenge.submitText(it.progress, value),
        )
    }

    fun onDigit(digit: Int) = _state.update {
        if (it.completed || it.calcInput.length >= MAX_DIGITS) it else it.copy(calcInput = it.calcInput + digit)
    }

    fun onBackspace() = _state.update { it.copy(calcInput = it.calcInput.dropLast(1)) }

    fun onSubmit() {
        val current = _state.value
        val value = current.calcInput.toIntOrNull() ?: return
        if (current.completed) return
        when (val answer = challenge.answer(current.progress, value)) {
            is RescueAnswer.Wrong -> _state.value = current.copy(
                progress = answer.progress,
                calcInput = "",
                wrongExpected = answer.expected,
                mistakes = current.mistakes + 1,
            )
            is RescueAnswer.Completed -> {
                _state.value = current.copy(progress = answer.progress, calcInput = "", wrongExpected = null)
                viewModelScope.launch { dismissWithRescue(answer.progress) }
            }
        }
    }

    private companion object {
        const val MAX_DIGITS = 3 // 49 + 49 = 98, 9 × 9 = 81
    }
}
