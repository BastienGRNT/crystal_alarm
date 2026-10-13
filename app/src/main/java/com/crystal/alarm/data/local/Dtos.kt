package com.crystal.alarm.data.local

import com.crystal.alarm.domain.model.DismissMethod
import com.crystal.alarm.domain.model.WakeCode
import com.crystal.alarm.domain.model.WakeUpRecord
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalTime

internal val StorageJson = Json { ignoreUnknownKeys = true }

@Serializable
internal data class WakeCodeDto(
    val id: String,
    val name: String,
    val value: String,
    val format: String,
) {
    fun toDomain() = WakeCode(id, name, value, format)
}

internal fun WakeCode.toDto() = WakeCodeDto(id, name, value, format)

@Serializable
internal data class ImportedRingtoneDto(
    val id: String,
    val name: String,
)

@Serializable
internal data class WakeUpRecordDto(
    val alarmMinuteOfDay: Int,
    val rangAt: Long,
    val stoppedAt: Long,
    val method: String,
) {
    fun toDomain() = WakeUpRecord(
        alarmTime = LocalTime.ofSecondOfDay(alarmMinuteOfDay * 60L),
        rangAt = Instant.ofEpochMilli(rangAt),
        stoppedAt = Instant.ofEpochMilli(stoppedAt),
        method = DismissMethod.valueOf(method),
    )
}

internal fun WakeUpRecord.toDto() = WakeUpRecordDto(
    alarmMinuteOfDay = alarmTime.hour * 60 + alarmTime.minute,
    rangAt = rangAt.toEpochMilli(),
    stoppedAt = stoppedAt.toEpochMilli(),
    method = method.name,
)
