package com.crystal.alarm.domain

import com.crystal.alarm.domain.model.Alarm
import com.crystal.alarm.domain.model.AlarmState
import com.crystal.alarm.domain.model.Lock
import com.crystal.alarm.domain.model.Ringtone
import com.crystal.alarm.domain.model.RingtoneKind
import com.crystal.alarm.domain.model.Settings
import com.crystal.alarm.domain.model.SoundSettings
import com.crystal.alarm.domain.model.WakeCode
import com.crystal.alarm.domain.port.AlarmScheduler
import com.crystal.alarm.domain.port.TimeSource
import com.crystal.alarm.domain.repository.AlarmRepository
import com.crystal.alarm.domain.repository.LockRepository
import com.crystal.alarm.domain.repository.RingtoneRepository
import com.crystal.alarm.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class FakeTimeSource(var instant: Instant, private val zoneId: ZoneId = ZoneId.of("Europe/Paris")) : TimeSource {
    override fun now(): Instant = instant
    override fun zone(): ZoneId = zoneId
    fun advance(duration: Duration) {
        instant = instant.plus(duration)
    }
}

class FakeAlarmRepository : AlarmRepository {
    override val state = MutableStateFlow<AlarmState>(AlarmState.None)
    override suspend fun current() = state.value
    override suspend fun schedule(alarm: Alarm) {
        state.value = AlarmState.Scheduled(alarm)
    }
    override suspend fun markRinging(alarm: Alarm, since: Instant) {
        state.value = AlarmState.Ringing(alarm, since)
    }
    override suspend fun clear() {
        state.value = AlarmState.None
    }
}

class FakeLockRepository : LockRepository {
    override val lock = MutableStateFlow<Lock?>(null)
    override suspend fun current() = lock.value
    override suspend fun set(lock: Lock) {
        this.lock.value = lock
    }
}

val TOOTHPASTE = WakeCode("toothpaste", "Dentifrice", "3017620422003", "EAN-13")

class FakeSettingsRepository(
    code: WakeCode? = TOOTHPASTE,
) : SettingsRepository {
    override val settings = MutableStateFlow(Settings(listOfNotNull(code), code?.id))
    override suspend fun current() = settings.value
    override suspend fun setWakeCodes(codes: List<WakeCode>, activeId: String?) {
        settings.value = settings.value.copy(wakeCodes = codes, activeWakeCodeId = activeId)
    }
    override suspend fun setLockMinutes(minutes: Int) {
        settings.value = settings.value.copy(lockMinutes = minutes)
    }
    override suspend fun setSound(sound: SoundSettings) {
        settings.value = settings.value.copy(sound = sound)
    }
}

class FakeRingtoneRepository : RingtoneRepository {
    override val ringtones = MutableStateFlow(
        listOf(
            Ringtone(SoundSettings.DEFAULT_RINGTONE_ID, "Forte", RingtoneKind.Builtin),
            Ringtone("system:default", "Téléphone", RingtoneKind.System),
        ),
    )
    override suspend fun import(source: String): Ringtone? {
        if (source.isBlank()) return null
        return Ringtone("imported:$source", source, RingtoneKind.Imported).also { ringtones.value += it }
    }
    override suspend fun delete(id: String) {
        ringtones.value = ringtones.value.filterNot { it.id == id }
    }
}

class FakeScheduler : AlarmScheduler {
    var scheduled: Alarm? = null
    var firedNow = 0
    var cancelled = 0
    override fun schedule(alarm: Alarm) {
        scheduled = alarm
    }
    override fun fireNow() {
        firedNow++
    }
    override fun cancel() {
        scheduled = null
        cancelled++
    }
}
