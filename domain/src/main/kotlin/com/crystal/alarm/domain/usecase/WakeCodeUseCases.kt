package com.crystal.alarm.domain.usecase

import com.crystal.alarm.domain.model.AlarmState
import com.crystal.alarm.domain.model.WakeCode
import com.crystal.alarm.domain.repository.AlarmRepository
import com.crystal.alarm.domain.repository.SettingsRepository
import java.util.UUID
import javax.inject.Inject

sealed interface AddWakeCodeResult {
    data class Added(val code: WakeCode) : AddWakeCodeResult
    data class AlreadySaved(val existing: WakeCode) : AddWakeCodeResult
    data object InvalidName : AddWakeCodeResult
    data object AlarmRinging : AddWakeCodeResult
}

/**
 * Saves a new named code and makes it the active one.
 * Codes can't be touched while ringing: no swapping codes to escape.
 */
class AddWakeCodeUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(name: String, value: String, format: String): AddWakeCodeResult {
        if (alarmRepository.current() is AlarmState.Ringing) return AddWakeCodeResult.AlarmRinging
        val cleanName = name.trim().take(WakeCode.MAX_NAME_LENGTH)
        val cleanValue = value.trim()
        if (cleanName.isEmpty() || cleanValue.isEmpty()) return AddWakeCodeResult.InvalidName

        val settings = settingsRepository.current()
        settings.wakeCodes.firstOrNull { it.value == cleanValue }
            ?.let { return AddWakeCodeResult.AlreadySaved(it) }

        val code = WakeCode(UUID.randomUUID().toString(), cleanName, cleanValue, format)
        settingsRepository.setWakeCodes(settings.wakeCodes + code, activeId = code.id)
        return AddWakeCodeResult.Added(code)
    }
}

/** Picks which saved code stops the alarm. */
class SelectWakeCodeUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(id: String): Boolean {
        if (alarmRepository.current() is AlarmState.Ringing) return false
        val settings = settingsRepository.current()
        if (settings.wakeCodes.none { it.id == id }) return false
        settingsRepository.setWakeCodes(settings.wakeCodes, activeId = id)
        return true
    }
}

class RenameWakeCodeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(id: String, name: String): Boolean {
        val cleanName = name.trim().take(WakeCode.MAX_NAME_LENGTH)
        if (cleanName.isEmpty()) return false
        val settings = settingsRepository.current()
        if (settings.wakeCodes.none { it.id == id }) return false
        settingsRepository.setWakeCodes(
            settings.wakeCodes.map { if (it.id == id) it.copy(name = cleanName) else it },
            settings.activeWakeCodeId,
        )
        return true
    }
}

sealed interface DeleteWakeCodeResult {
    data object Deleted : DeleteWakeCodeResult
    /** Deleting it would leave a scheduled alarm with nothing to scan. */
    data object LastCodeWithAlarm : DeleteWakeCodeResult
    data object AlarmRinging : DeleteWakeCodeResult
    data object NotFound : DeleteWakeCodeResult
}

/** Deletes a code. If it was active, the first remaining code takes over. */
class DeleteWakeCodeUseCase @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(id: String): DeleteWakeCodeResult {
        val alarm = alarmRepository.current()
        if (alarm is AlarmState.Ringing) return DeleteWakeCodeResult.AlarmRinging
        val settings = settingsRepository.current()
        if (settings.wakeCodes.none { it.id == id }) return DeleteWakeCodeResult.NotFound

        val remaining = settings.wakeCodes.filterNot { it.id == id }
        if (remaining.isEmpty() && alarm is AlarmState.Scheduled) return DeleteWakeCodeResult.LastCodeWithAlarm

        val activeId = settings.activeWakeCodeId.takeIf { it != id } ?: remaining.firstOrNull()?.id
        settingsRepository.setWakeCodes(remaining, activeId)
        return DeleteWakeCodeResult.Deleted
    }
}
