package com.crystal.alarm.domain.repository

import com.crystal.alarm.domain.model.Ringtone
import kotlinx.coroutines.flow.Flow

/** Built-in, imported and system ringtones. */
interface RingtoneRepository {
    val ringtones: Flow<List<Ringtone>>

    /** Copies the audio file at [source] (a platform URI) into the app. Null if it isn't playable. */
    suspend fun import(source: String): Ringtone?

    /** Only imported ringtones can be deleted. */
    suspend fun delete(id: String)
}
