package com.crystal.alarm.domain.repository

import com.crystal.alarm.domain.model.Lock
import kotlinx.coroutines.flow.Flow

interface LockRepository {
    /** Last lock written, possibly already expired. */
    val lock: Flow<Lock?>

    suspend fun current(): Lock?

    suspend fun set(lock: Lock)
}
