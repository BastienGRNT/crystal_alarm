package com.crystal.alarm.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.crystal.alarm.data.local.PreferenceKeys
import com.crystal.alarm.data.local.StorageJson
import com.crystal.alarm.data.local.WakeCodeDto
import com.crystal.alarm.data.local.safeData
import com.crystal.alarm.data.local.toDto
import com.crystal.alarm.domain.model.Settings
import com.crystal.alarm.domain.model.SoundSettings
import com.crystal.alarm.domain.model.WakeCode
import com.crystal.alarm.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val store: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<Settings> = store.safeData
        .map { prefs ->
            val (codes, activeId) = prefs.readWakeCodes()
            Settings(
                wakeCodes = codes,
                activeWakeCodeId = activeId,
                lockMinutes = prefs[PreferenceKeys.LOCK_MINUTES] ?: Settings.DEFAULT_LOCK_MINUTES,
                sound = SoundSettings(
                    ringtoneId = prefs[PreferenceKeys.SOUND_RINGTONE_ID] ?: SoundSettings.DEFAULT_RINGTONE_ID,
                    volumePercent = prefs[PreferenceKeys.SOUND_VOLUME] ?: SoundSettings.DEFAULT_VOLUME,
                    progressive = prefs[PreferenceKeys.SOUND_PROGRESSIVE] ?: false,
                    vibrate = prefs[PreferenceKeys.SOUND_VIBRATE] ?: true,
                ),
            )
        }
        .distinctUntilChanged()

    override suspend fun current(): Settings = settings.first()

    override suspend fun setWakeCodes(codes: List<WakeCode>, activeId: String?) {
        store.edit {
            it[PreferenceKeys.WAKE_CODES_JSON] = StorageJson.encodeToString(codes.map { code -> code.toDto() })
            if (activeId != null) it[PreferenceKeys.ACTIVE_WAKE_CODE_ID] = activeId else it.remove(PreferenceKeys.ACTIVE_WAKE_CODE_ID)
            it.remove(PreferenceKeys.LEGACY_WAKE_CODE_VALUE)
            it.remove(PreferenceKeys.LEGACY_WAKE_CODE_FORMAT)
        }
    }

    override suspend fun setLockMinutes(minutes: Int) {
        store.edit { it[PreferenceKeys.LOCK_MINUTES] = minutes }
    }

    override suspend fun setSound(sound: SoundSettings) {
        store.edit {
            it[PreferenceKeys.SOUND_RINGTONE_ID] = sound.ringtoneId
            it[PreferenceKeys.SOUND_VOLUME] = sound.volumePercent
            it[PreferenceKeys.SOUND_PROGRESSIVE] = sound.progressive
            it[PreferenceKeys.SOUND_VIBRATE] = sound.vibrate
        }
    }

    private fun Preferences.readWakeCodes(): Pair<List<WakeCode>, String?> {
        val json = this[PreferenceKeys.WAKE_CODES_JSON]
        if (json == null) {
            // v1.0 stored a single unnamed code.
            val legacy = this[PreferenceKeys.LEGACY_WAKE_CODE_VALUE]?.takeIf { it.isNotBlank() }
                ?: return emptyList<WakeCode>() to null
            val code = WakeCode(LEGACY_ID, LEGACY_NAME, legacy, this[PreferenceKeys.LEGACY_WAKE_CODE_FORMAT].orEmpty())
            return listOf(code) to code.id
        }
        val codes = try {
            StorageJson.decodeFromString<List<WakeCodeDto>>(json).mapNotNull { dto ->
                runCatching { dto.toDomain() }.getOrNull()
            }
        } catch (_: SerializationException) {
            emptyList()
        }
        return codes to this[PreferenceKeys.ACTIVE_WAKE_CODE_ID]
    }

    private companion object {
        const val LEGACY_ID = "legacy"
        const val LEGACY_NAME = "Mon code"
    }
}
