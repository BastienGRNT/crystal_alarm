package com.crystal.alarm.data.ringtone

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.crystal.alarm.R
import com.crystal.alarm.data.local.ImportedRingtoneDto
import com.crystal.alarm.data.local.PreferenceKeys
import com.crystal.alarm.data.local.StorageJson
import com.crystal.alarm.data.local.safeData
import com.crystal.alarm.domain.model.Ringtone
import com.crystal.alarm.domain.model.RingtoneKind
import com.crystal.alarm.domain.model.SoundSettings
import com.crystal.alarm.domain.repository.RingtoneRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Built-in sounds live in res/raw, imported ones are copied into the app's private storage
 * (so they keep working even if the original file is deleted or the permission revoked).
 */
@Singleton
class DataStoreRingtoneRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: DataStore<Preferences>,
) : RingtoneRepository {

    private val directory: File get() = File(context.filesDir, "ringtones").apply { mkdirs() }

    override val ringtones: Flow<List<Ringtone>> = store.safeData
        .map { prefs -> builtins() + systemRingtone() + prefs.imported().map { it.toRingtone() } }
        .distinctUntilChanged()

    override suspend fun import(source: String): Ringtone? = withContext(Dispatchers.IO) {
        val uri = source.toUri()
        val id = UUID.randomUUID().toString()
        val target = File(directory, id)
        val copied = runCatching {
            val input = context.contentResolver.openInputStream(uri) ?: return@runCatching false
            input.use { stream -> target.outputStream().use { stream.copyTo(it) } }
            target.length() in 1..MAX_IMPORT_BYTES
        }.getOrDefault(false)

        if (!copied || !isPlayable(target)) {
            target.delete()
            return@withContext null
        }
        val dto = ImportedRingtoneDto(id = "$IMPORTED_PREFIX$id", name = displayName(uri))
        store.edit { prefs ->
            prefs[PreferenceKeys.IMPORTED_RINGTONES_JSON] = StorageJson.encodeToString(prefs.imported() + dto)
        }
        dto.toRingtone()
    }

    override suspend fun delete(id: String) {
        if (!id.startsWith(IMPORTED_PREFIX)) return
        withContext(Dispatchers.IO) { fileFor(id).delete() }
        store.edit { prefs ->
            prefs[PreferenceKeys.IMPORTED_RINGTONES_JSON] = StorageJson.encodeToString(prefs.imported().filterNot { it.id == id })
        }
    }

    /**
     * Playable URIs for [id], best first, ending with safe fallbacks
     * (default built-in, then the phone's alarm sound). Never empty.
     */
    fun playbackCandidates(id: String): List<Uri> = buildList {
        uriFor(id)?.let(::add)
        uriFor(SoundSettings.DEFAULT_RINGTONE_ID)?.let(::add)
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.let(::add)
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)?.let(::add)
    }.distinct()

    private fun uriFor(id: String): Uri? = when {
        id == SYSTEM_ID -> RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        id.startsWith(IMPORTED_PREFIX) -> fileFor(id).takeIf { it.exists() }?.let(Uri::fromFile)
        else -> BuiltinRingtone.fromId(id)?.let { rawUri(it.raw) }
    }

    private fun rawUri(raw: Int): Uri = Uri.Builder()
        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(context.packageName)
        .appendPath(raw.toString())
        .build()

    private fun fileFor(id: String) = File(directory, id.removePrefix(IMPORTED_PREFIX))

    private fun builtins() = BuiltinRingtone.entries.map {
        Ringtone(it.id, context.getString(it.title), RingtoneKind.Builtin, context.getString(it.description))
    }

    private fun systemRingtone() = Ringtone(
        SYSTEM_ID,
        context.getString(R.string.ringtone_system),
        RingtoneKind.System,
        context.getString(R.string.ringtone_system_description),
    )

    private fun ImportedRingtoneDto.toRingtone() = Ringtone(id, name, RingtoneKind.Imported)

    private fun Preferences.imported(): List<ImportedRingtoneDto> {
        val json = this[PreferenceKeys.IMPORTED_RINGTONES_JSON] ?: return emptyList()
        return try {
            StorageJson.decodeFromString(json)
        } catch (_: SerializationException) {
            emptyList()
        }
    }

    private fun isPlayable(file: File): Boolean = runCatching {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes"
        }
    }.getOrDefault(false)

    private fun displayName(uri: Uri): String {
        val raw = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
        return raw?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }?.take(40)
            ?: context.getString(R.string.ringtone_imported_default_name)
    }

    companion object {
        const val SYSTEM_ID = "system:default"
        private const val IMPORTED_PREFIX = "imported:"
        private const val MAX_IMPORT_BYTES = 30L * 1024 * 1024
    }
}
