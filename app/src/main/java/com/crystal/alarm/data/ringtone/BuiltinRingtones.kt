package com.crystal.alarm.data.ringtone

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import com.crystal.alarm.R
import com.crystal.alarm.domain.model.RingtoneCategory

/**
 * Ringtones shipped with the app: short loops synthesized by tools/generate_ringtones.py,
 * and royalty-free Pixabay tracks (origin in tools/ringtones_credits.md).
 * Keys are stored in the settings: never rename one.
 */
internal enum class BuiltinRingtone(
    val key: String,
    @param:RawRes val raw: Int,
    @param:StringRes val title: Int,
    @param:StringRes val description: Int,
    val category: RingtoneCategory,
) {
    Forte("forte", R.raw.ringtone_forte, R.string.ringtone_forte, R.string.ringtone_forte_description, RingtoneCategory.Classic),
    Cloche("cloche", R.raw.ringtone_cloche, R.string.ringtone_cloche, R.string.ringtone_cloche_description, RingtoneCategory.Classic),
    Sirene("sirene", R.raw.ringtone_sirene, R.string.ringtone_sirene, R.string.ringtone_sirene_description, RingtoneCategory.Classic),
    Hard("hard", R.raw.ringtone_hard, R.string.ringtone_hard, R.string.ringtone_hard_description, RingtoneCategory.Classic),

    Calme("calme", R.raw.ringtone_calme, R.string.ringtone_calme, R.string.ringtone_calme_description, RingtoneCategory.Soft),
    Aube("aube", R.raw.ringtone_aube, R.string.ringtone_aube, R.string.ringtone_aube_description, RingtoneCategory.Soft),

    Cristal("cristal", R.raw.ringtone_cristal, R.string.ringtone_cristal, R.string.ringtone_cristal_description, RingtoneCategory.Electro),
    Etincelles("etincelles", R.raw.ringtone_etincelles, R.string.ringtone_etincelles, R.string.ringtone_etincelles_description, RingtoneCategory.Electro),

    Rave("rave", R.raw.ringtone_rave, R.string.ringtone_rave, R.string.ringtone_rave_description, RingtoneCategory.Techno),
    SousSol("sous_sol", R.raw.ringtone_sous_sol, R.string.ringtone_sous_sol, R.string.ringtone_sous_sol_description, RingtoneCategory.Techno),

    BaileFunk("baile_funk", R.raw.ringtone_baile_funk, R.string.ringtone_baile_funk, R.string.ringtone_baile_funk_description, RingtoneCategory.FunkPop),
    Kpop("kpop", R.raw.ringtone_kpop, R.string.ringtone_kpop, R.string.ringtone_kpop_description, RingtoneCategory.FunkPop),
    ElectroFunky("electro_funky", R.raw.ringtone_electro_funky, R.string.ringtone_electro_funky, R.string.ringtone_electro_funky_description, RingtoneCategory.FunkPop),
    PopFraiche("pop_fraiche", R.raw.ringtone_pop_fraiche, R.string.ringtone_pop_fraiche, R.string.ringtone_pop_fraiche_description, RingtoneCategory.FunkPop),
    ;

    val id: String get() = "$BUILTIN_PREFIX$key"

    companion object {
        const val BUILTIN_PREFIX = "builtin:"
        fun fromId(id: String): BuiltinRingtone? = entries.firstOrNull { it.id == id }
    }
}
