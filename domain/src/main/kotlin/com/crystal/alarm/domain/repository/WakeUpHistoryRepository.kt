package com.crystal.alarm.domain.repository

import com.crystal.alarm.domain.model.WakeUpRecord
import kotlinx.coroutines.flow.Flow

interface WakeUpHistoryRepository {
    /** Newest first, at most [WakeUpRecord.MAX_RECORDS]. */
    val records: Flow<List<WakeUpRecord>>

    /** Stores [record] first and drops the oldest ones beyond [WakeUpRecord.MAX_RECORDS]. */
    suspend fun add(record: WakeUpRecord)
}
