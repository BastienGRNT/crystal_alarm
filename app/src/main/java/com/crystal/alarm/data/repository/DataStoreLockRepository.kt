package com.crystal.alarm.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.crystal.alarm.data.local.PreferenceKeys
import com.crystal.alarm.data.local.safeData
import com.crystal.alarm.domain.model.Lock
import com.crystal.alarm.domain.repository.LockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreLockRepository @Inject constructor(
    private val store: DataStore<Preferences>,
) : LockRepository {

    override val lock: Flow<Lock?> = store.safeData
        .map { prefs ->
            val since = prefs[PreferenceKeys.LOCK_SINCE]
            val until = prefs[PreferenceKeys.LOCK_UNTIL]
            if (since != null && until != null) {
                Lock(Instant.ofEpochMilli(since), Instant.ofEpochMilli(until))
            } else {
                null
            }
        }
        .distinctUntilChanged()

    override suspend fun current(): Lock? = lock.first()

    override suspend fun set(lock: Lock) {
        store.edit {
            it[PreferenceKeys.LOCK_SINCE] = lock.since.toEpochMilli()
            it[PreferenceKeys.LOCK_UNTIL] = lock.until.toEpochMilli()
        }
    }
}
