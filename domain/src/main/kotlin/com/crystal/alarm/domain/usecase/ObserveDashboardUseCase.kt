package com.crystal.alarm.domain.usecase

import com.crystal.alarm.domain.model.AlarmState
import com.crystal.alarm.domain.model.Lock
import com.crystal.alarm.domain.model.Settings
import com.crystal.alarm.domain.repository.AlarmRepository
import com.crystal.alarm.domain.repository.LockRepository
import com.crystal.alarm.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** Everything the UI and the widget need, as a single stream. */
data class Dashboard(
    val alarm: AlarmState,
    val lock: Lock?,
    val settings: Settings,
)

class ObserveDashboardUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val lockRepository: LockRepository,
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(): Flow<Dashboard> = combine(
        alarmRepository.state,
        lockRepository.lock,
        settingsRepository.settings,
        ::Dashboard,
    )
}
