package com.crystal.alarm.domain.model

enum class RingtoneKind { Builtin, Imported, System }

data class Ringtone(
    val id: String,
    val name: String,
    val kind: RingtoneKind,
    val description: String? = null,
)
