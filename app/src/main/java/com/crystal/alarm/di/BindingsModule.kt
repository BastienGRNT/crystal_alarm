package com.crystal.alarm.di

import com.crystal.alarm.data.repository.DataStoreAlarmRepository
import com.crystal.alarm.data.repository.DataStoreLockRepository
import com.crystal.alarm.data.repository.DataStoreSettingsRepository
import com.crystal.alarm.data.repository.DataStoreWakeUpHistoryRepository
import com.crystal.alarm.data.ringtone.DataStoreRingtoneRepository
import com.crystal.alarm.domain.port.AlarmScheduler
import com.crystal.alarm.domain.port.RingtonePreviewer
import com.crystal.alarm.domain.port.TimeSource
import com.crystal.alarm.domain.repository.AlarmRepository
import com.crystal.alarm.domain.repository.LockRepository
import com.crystal.alarm.domain.repository.RingtoneRepository
import com.crystal.alarm.domain.repository.SettingsRepository
import com.crystal.alarm.domain.repository.WakeUpHistoryRepository
import com.crystal.alarm.platform.alarm.AndroidAlarmScheduler
import com.crystal.alarm.platform.sound.AndroidRingtonePreviewer
import com.crystal.alarm.platform.time.SystemTimeSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Wires domain interfaces (repositories and ports) to their Android implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {

    @Binds
    abstract fun bindAlarmRepository(impl: DataStoreAlarmRepository): AlarmRepository

    @Binds
    abstract fun bindLockRepository(impl: DataStoreLockRepository): LockRepository

    @Binds
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    abstract fun bindWakeUpHistoryRepository(impl: DataStoreWakeUpHistoryRepository): WakeUpHistoryRepository

    @Binds
    abstract fun bindRingtoneRepository(impl: DataStoreRingtoneRepository): RingtoneRepository

    @Binds
    abstract fun bindRingtonePreviewer(impl: AndroidRingtonePreviewer): RingtonePreviewer

    @Binds
    abstract fun bindAlarmScheduler(impl: AndroidAlarmScheduler): AlarmScheduler

    @Binds
    abstract fun bindTimeSource(impl: SystemTimeSource): TimeSource
}
