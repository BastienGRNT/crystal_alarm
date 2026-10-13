package com.crystal.alarm.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.crystal.alarm.data.local.PreferenceKeys
import com.crystal.alarm.data.local.StorageJson
import com.crystal.alarm.data.local.WakeUpRecordDto
import com.crystal.alarm.data.local.safeData
import com.crystal.alarm.data.local.toDto
import com.crystal.alarm.domain.model.WakeUpRecord
import com.crystal.alarm.domain.repository.WakeUpHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreWakeUpHistoryRepository @Inject constructor(
    private val store: DataStore<Preferences>,
) : WakeUpHistoryRepository {

    override val records: Flow<List<WakeUpRecord>> = store.safeData
        .map { it.readRecords() }
        .distinctUntilChanged()

    override suspend fun add(record: WakeUpRecord) {
        store.edit { prefs ->
            val updated = (listOf(record) + prefs.readRecords()).take(WakeUpRecord.MAX_RECORDS)
            prefs[PreferenceKeys.WAKE_UP_HISTORY_JSON] = StorageJson.encodeToString(updated.map { it.toDto() })
        }
    }

    /** A broken entry is skipped rather than losing the whole history. */
    private fun Preferences.readRecords(): List<WakeUpRecord> {
        val json = this[PreferenceKeys.WAKE_UP_HISTORY_JSON] ?: return emptyList()
        return try {
            StorageJson.decodeFromString<List<WakeUpRecordDto>>(json).mapNotNull { dto ->
                runCatching { dto.toDomain() }.getOrNull()
            }
        } catch (_: SerializationException) {
            emptyList()
        }
    }
}
