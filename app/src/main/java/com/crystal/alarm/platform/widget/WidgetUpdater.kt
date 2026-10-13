package com.crystal.alarm.platform.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.crystal.alarm.di.ApplicationScope
import com.crystal.alarm.domain.port.TimeSource
import com.crystal.alarm.domain.usecase.Dashboard
import com.crystal.alarm.domain.usecase.ObserveDashboardUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Pushes the dashboard state to every placed widget, and keeps the minute ticker in sync. */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val observeDashboard: ObserveDashboardUseCase,
    private val timeSource: TimeSource,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val mutex = Mutex()

    /** Called once per process: any state change made by the app is reflected on the widget. */
    fun startSync() {
        scope.launch { observeDashboard().collect { refresh(it) } }
    }

    suspend fun refresh() = refresh(observeDashboard().first())

    private suspend fun refresh(dashboard: Dashboard) = mutex.withLock {
        val ids = GlanceAppWidgetManager(context).getGlanceIds(NextAlarmWidget::class.java)
        if (ids.isEmpty()) {
            WidgetTicker.cancel(context)
            return@withLock
        }
        val now = timeSource.now()
        val snapshot = WidgetSnapshot.from(context, dashboard, now, timeSource.zone())
        val widget = NextAlarmWidget()
        ids.forEach { id ->
            updateAppWidgetState(context, id) { snapshot.writeTo(it) }
            widget.update(context, id)
        }
        if (snapshot.needsTicking) {
            WidgetTicker.scheduleNextMinute(context, now.toEpochMilli())
        } else {
            WidgetTicker.cancel(context)
        }
    }
}
