package com.crystal.alarm.domain.repository

import com.crystal.alarm.domain.model.Settings
import com.crystal.alarm.domain.model.SoundSettings
import com.crystal.alarm.domain.model.WakeCode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<Settings>

    suspend fun current(): Settings

    suspend fun setWakeCodes(codes: List<WakeCode>, activeId: String?)

    suspend fun setLockMinutes(minutes: Int)

    suspend fun setSound(sound: SoundSettings)
}
