package com.crystal.alarm.presentation.sound

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crystal.alarm.domain.model.Ringtone
import com.crystal.alarm.domain.model.SoundSettings
import com.crystal.alarm.domain.usecase.DeleteRingtoneUseCase
import com.crystal.alarm.domain.usecase.ImportRingtoneUseCase
import com.crystal.alarm.domain.usecase.ObserveDashboardUseCase
import com.crystal.alarm.domain.usecase.ObserveRingtonesUseCase
import com.crystal.alarm.domain.usecase.PreviewRingtoneUseCase
import com.crystal.alarm.domain.usecase.UpdateSoundSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SoundUiState(
    val sound: SoundSettings? = null,
    val ringtones: List<Ringtone> = emptyList(),
    val previewingId: String? = null,
    val importing: Boolean = false,
    val importFailed: Boolean = false,
)

private data class Transient(val previewingId: String? = null, val importing: Boolean = false, val importFailed: Boolean = false)

@HiltViewModel
class SoundViewModel @Inject constructor(
    observeDashboard: ObserveDashboardUseCase,
    observeRingtones: ObserveRingtonesUseCase,
    private val updateSound: UpdateSoundSettingsUseCase,
    private val importRingtone: ImportRingtoneUseCase,
    private val deleteRingtone: DeleteRingtoneUseCase,
    private val preview: PreviewRingtoneUseCase,
) : ViewModel() {

    private val transient = MutableStateFlow(Transient())
    private var previewJob: Job? = null

    val state: StateFlow<SoundUiState> = combine(
        observeDashboard().map { it.settings.sound },
        observeRingtones(),
        transient,
    ) { sound, ringtones, t ->
        SoundUiState(sound, ringtones, t.previewingId, t.importing, t.importFailed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SoundUiState())

    private val sound: SoundSettings get() = state.value.sound ?: SoundSettings()

    fun onSelect(ringtone: Ringtone) {
        save(sound.copy(ringtoneId = ringtone.id))
        play(ringtone.id, sound.volumePercent)
    }

    fun onTogglePreview(ringtone: Ringtone) {
        if (transient.value.previewingId == ringtone.id) stopPreview() else play(ringtone.id, sound.volumePercent)
    }

    fun onVolumeChange(volumePercent: Int) {
        save(sound.copy(volumePercent = volumePercent))
        play(sound.ringtoneId, volumePercent)
    }

    fun onProgressiveChange(progressive: Boolean) = save(sound.copy(progressive = progressive))

    fun onVibrateChange(vibrate: Boolean) = save(sound.copy(vibrate = vibrate))

    fun onImport(uri: String) {
        stopPreview()
        viewModelScope.launch {
            transient.value = transient.value.copy(importing = true, importFailed = false)
            val imported = importRingtone(uri)
            transient.value = transient.value.copy(importing = false, importFailed = imported == null)
        }
    }

    fun onDelete(ringtone: Ringtone) {
        if (transient.value.previewingId == ringtone.id) stopPreview()
        viewModelScope.launch { deleteRingtone(ringtone) }
    }

    fun onImportErrorShown() {
        transient.value = transient.value.copy(importFailed = false)
    }

    fun stopPreview() {
        previewJob?.cancel()
        preview.stop()
        transient.value = transient.value.copy(previewingId = null)
    }

    private fun play(ringtoneId: String, volumePercent: Int) {
        previewJob?.cancel()
        preview.play(ringtoneId, volumePercent)
        transient.value = transient.value.copy(previewingId = ringtoneId)
        // The previewer stops by itself after a few seconds; mirror it in the UI.
        previewJob = viewModelScope.launch {
            delay(PREVIEW_UI_MS)
            transient.value = transient.value.copy(previewingId = null)
        }
    }

    private fun save(updated: SoundSettings) {
        viewModelScope.launch { updateSound(updated) }
    }

    override fun onCleared() {
        preview.stop()
    }

    private companion object {
        const val PREVIEW_UI_MS = 6_000L
    }
}
