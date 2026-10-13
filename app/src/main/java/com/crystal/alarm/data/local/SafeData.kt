package com.crystal.alarm.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.IOException

/** A corrupted/unreadable file must never crash the alarm: fall back to empty preferences. */
internal val DataStore<Preferences>.safeData: Flow<Preferences>
    get() = data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }
