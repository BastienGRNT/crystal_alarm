package com.crystal.alarm.domain.usecase

import com.crystal.alarm.domain.model.Ringtone
import com.crystal.alarm.domain.model.RingtoneKind
import com.crystal.alarm.domain.model.SoundSettings
import com.crystal.alarm.domain.port.RingtonePreviewer
import com.crystal.alarm.domain.repository.RingtoneRepository
import com.crystal.alarm.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveRingtonesUseCase @Inject constructor(
    private val ringtoneRepository: RingtoneRepository,
) {
    operator fun invoke(): Flow<List<Ringtone>> = ringtoneRepository.ringtones
}

/** Saves the sound settings, with the volume snapped to its step and kept audible. */
class UpdateSoundSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(sound: SoundSettings) {
        val step = SoundSettings.VOLUME_STEP
        val volume = ((sound.volumePercent + step / 2) / step * step)
            .coerceIn(SoundSettings.MIN_VOLUME, SoundSettings.MAX_VOLUME)
        settingsRepository.setSound(sound.copy(volumePercent = volume))
    }
}

/** Imports an audio file and selects it right away. */
class ImportRingtoneUseCase @Inject constructor(
    private val ringtoneRepository: RingtoneRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(source: String): Ringtone? {
        val ringtone = ringtoneRepository.import(source) ?: return null
        val sound = settingsRepository.current().sound
        settingsRepository.setSound(sound.copy(ringtoneId = ringtone.id))
        return ringtone
    }
}

/** Deletes an imported ringtone; falls back to the default one if it was selected. */
class DeleteRingtoneUseCase @Inject constructor(
    private val ringtoneRepository: RingtoneRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(ringtone: Ringtone): Boolean {
        if (ringtone.kind != RingtoneKind.Imported) return false
        val sound = settingsRepository.current().sound
        if (sound.ringtoneId == ringtone.id) {
            settingsRepository.setSound(sound.copy(ringtoneId = SoundSettings.DEFAULT_RINGTONE_ID))
        }
        ringtoneRepository.delete(ringtone.id)
        return true
    }
}

class PreviewRingtoneUseCase @Inject constructor(
    private val previewer: RingtonePreviewer,
) {
    fun play(ringtoneId: String, volumePercent: Int) = previewer.play(ringtoneId, volumePercent)

    fun stop() = previewer.stop()
}
