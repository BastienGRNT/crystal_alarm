package com.crystal.alarm.di

import android.content.Context
import com.crystal.alarm.domain.usecase.RestoreAlarmUseCase
import com.crystal.alarm.platform.widget.WidgetUpdater
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope

/** Dependencies for BroadcastReceivers / widget receivers, which Hilt can't constructor-inject. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReceiverEntryPoint {
    fun restoreAlarm(): RestoreAlarmUseCase
    fun widgetUpdater(): WidgetUpdater

    @ApplicationScope
    fun applicationScope(): CoroutineScope

    companion object {
        fun get(context: Context): ReceiverEntryPoint =
            EntryPointAccessors.fromApplication(context.applicationContext, ReceiverEntryPoint::class.java)
    }
}
