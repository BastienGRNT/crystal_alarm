package com.crystal.alarm.domain.usecase

import com.crystal.alarm.domain.model.DismissMethod
import com.crystal.alarm.domain.rescue.RescueProgress
import javax.inject.Inject

/** Last resort when the wake code is out of reach: only a fully completed rescue stops the alarm. */
class DismissWithRescueUseCase @Inject constructor(
    private val finishRinging: FinishRingingUseCase,
) {
    suspend operator fun invoke(progress: RescueProgress): DismissResult {
        if (!progress.isComplete) return DismissResult.WrongCode
        return if (finishRinging(DismissMethod.Rescue)) DismissResult.Dismissed else DismissResult.NotRinging
    }
}
