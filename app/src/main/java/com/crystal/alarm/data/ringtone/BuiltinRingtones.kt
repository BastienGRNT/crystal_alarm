package com.crystal.alarm.data.ringtone

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import com.crystal.alarm.R

/** Ringtones shipped with the app (synthesized by tools/generate_ringtones.py). */
internal enum class BuiltinRingtone(
    val key: String,
    @param:RawRes val raw: Int,
    @param:StringRes val title: Int,
    @param:StringRes val description: Int,
) {
    Hard("hard", R.raw.ringtone_hard, R.string.ringtone_hard, R.string.ringtone_hard_description),
    Forte("forte", R.raw.ringtone_forte, R.string.ringtone_forte, R.string.ringtone_forte_description),
    Sirene("sirene", R.raw.ringtone_sirene, R.string.ringtone_sirene, R.string.ringtone_sirene_description),
    Cloche("cloche", R.raw.ringtone_cloche, R.string.ringtone_cloche, R.string.ringtone_cloche_description),
    Calme("calme", R.raw.ringtone_calme, R.string.ringtone_calme, R.string.ringtone_calme_description),
    Aube("aube", R.raw.ringtone_aube, R.string.ringtone_aube, R.string.ringtone_aube_description),
    ;

    val id: String get() = "$BUILTIN_PREFIX$key"

    companion object {
        const val BUILTIN_PREFIX = "builtin:"
        fun fromId(id: String): BuiltinRingtone? = entries.firstOrNull { it.id == id }
    }
}
