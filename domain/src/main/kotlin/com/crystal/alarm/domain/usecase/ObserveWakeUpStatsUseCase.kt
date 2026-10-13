package com.crystal.alarm.domain.usecase

import com.crystal.alarm.domain.model.DismissMethod
import com.crystal.alarm.domain.model.WakeUpRecord
import com.crystal.alarm.domain.repository.WakeUpHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Duration
import javax.inject.Inject

/** Wake-up history and its summary. [average] and [best] are null until the first record. */
data class WakeUpStats(
    val records: List<WakeUpRecord>,
    /** Mean over the last [RECENT_COUNT] mornings. */
    val average: Duration?,
    val best: Duration?,
    val rescueCount: Int,
) {
    val last: WakeUpRecord? get() = records.firstOrNull()

    companion object {
        const val RECENT_COUNT = 7
    }
}

class ObserveWakeUpStatsUseCase @Inject constructor(
    private val historyRepository: WakeUpHistoryRepository,
) {
    operator fun invoke(): Flow<WakeUpStats> = historyRepository.records.map { records ->
        val recent = records.take(WakeUpStats.RECENT_COUNT)
        WakeUpStats(
            records = records,
            average = if (recent.isEmpty()) null else Duration.ofSeconds(recent.sumOf { it.duration.seconds } / recent.size),
            best = records.minOfOrNull { it.duration },
            rescueCount = records.count { it.method == DismissMethod.Rescue },
        )
    }
}
