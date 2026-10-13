package com.crystal.alarm.domain

import com.crystal.alarm.domain.model.SoundSettings
import com.crystal.alarm.domain.usecase.DeleteRingtoneUseCase
import com.crystal.alarm.domain.usecase.ImportRingtoneUseCase
import com.crystal.alarm.domain.usecase.UpdateSoundSettingsUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class SoundUseCasesTest {
    private val settings = FakeSettingsRepository()
    private val ringtones = FakeRingtoneRepository()

    @Test
    fun `volume is snapped and never too low`() = runTest {
        val update = UpdateSoundSettingsUseCase(settings)
        update(SoundSettings(volumePercent = 5))
        assertEquals(SoundSettings.MIN_VOLUME, settings.current().sound.volumePercent)
        update(SoundSettings(volumePercent = 74, progressive = true))
        assertEquals(70, settings.current().sound.volumePercent)
        assertEquals(true, settings.current().sound.progressive)
    }

    @Test
    fun `imported ringtone is selected, deleting it restores the default`() = runTest {
        val imported = ImportRingtoneUseCase(ringtones, settings)("ma-musique")!!
        assertEquals(imported.id, settings.current().sound.ringtoneId)

        DeleteRingtoneUseCase(ringtones, settings)(imported)
        assertEquals(SoundSettings.DEFAULT_RINGTONE_ID, settings.current().sound.ringtoneId)
    }

    @Test
    fun `built-in ringtones cannot be deleted and failed imports change nothing`() = runTest {
        assertFalse(DeleteRingtoneUseCase(ringtones, settings)(ringtones.ringtones.value.first()))
        assertNull(ImportRingtoneUseCase(ringtones, settings)(""))
        assertEquals(SoundSettings.DEFAULT_RINGTONE_ID, settings.current().sound.ringtoneId)
    }
}
