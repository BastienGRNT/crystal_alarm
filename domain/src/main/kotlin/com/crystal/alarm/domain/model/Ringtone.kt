package com.crystal.alarm.domain.model

enum class RingtoneKind { Builtin, Imported, System }

/** Groups of the ringtone list, shown in this order. */
enum class RingtoneCategory { Classic, Soft, Electro, Techno, FunkPop, Phone, Mine }

data class Ringtone(
    val id: String,
    val name: String,
    val kind: RingtoneKind,
    val description: String? = null,
    val category: RingtoneCategory = when (kind) {
        RingtoneKind.Builtin -> RingtoneCategory.Classic
        RingtoneKind.System -> RingtoneCategory.Phone
        RingtoneKind.Imported -> RingtoneCategory.Mine
    },
)
