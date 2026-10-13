package com.crystal.alarm.data.local

import com.crystal.alarm.domain.model.WakeCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
