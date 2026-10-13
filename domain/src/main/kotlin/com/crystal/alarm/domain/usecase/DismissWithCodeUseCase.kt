package com.crystal.alarm.domain.usecase

import com.crystal.alarm.domain.model.AlarmState
import com.crystal.alarm.domain.model.DismissMethod
import com.crystal.alarm.domain.repository.AlarmRepository
import com.crystal.alarm.domain.repository.SettingsRepository
import javax.inject.Inject

sealed interface DismissResult {
    data object Dismissed : DismissResult
    data object WrongCode : DismissResult
    data object NotRinging : DismissResult
}

class DismissWithCodeUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val settingsRepository: SettingsRepository,
    private val finishRinging: FinishRingingUseCase,
) {
    suspend operator fun invoke(scanned: String): DismissResult {
        if (alarmRepository.current() !is AlarmState.Ringing) return DismissResult.NotRinging
        val code = settingsRepository.current().activeWakeCode
        if (code == null || !code.matches(scanned)) return DismissResult.WrongCode
        return if (finishRinging(DismissMethod.Code)) DismissResult.Dismissed else DismissResult.NotRinging
    }
}
