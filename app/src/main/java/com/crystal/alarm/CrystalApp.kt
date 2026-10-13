package com.crystal.alarm

import android.app.Application
import com.crystal.alarm.di.ApplicationScope
import com.crystal.alarm.domain.usecase.RestoreAlarmUseCase
import com.crystal.alarm.platform.alarm.AlarmNotifications
import com.crystal.alarm.platform.widget.WidgetUpdater
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class CrystalApp : Application() {

    @Inject lateinit var widgetUpdater: WidgetUpdater
    @Inject lateinit var restoreAlarm: RestoreAlarmUseCase
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        AlarmNotifications.createChannel(this)
        widgetUpdater.startSync()
        // A force-stop wipes AlarmManager alarms: re-arm on every process start (idempotent).
        applicationScope.launch { restoreAlarm() }
    }
}
