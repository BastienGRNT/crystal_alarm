package com.crystal.alarm.platform.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.crystal.alarm.di.ReceiverEntryPoint
import kotlinx.coroutines.launch

class NextAlarmWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = NextAlarmWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        refresh(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        refresh(context)
    }

    // No goAsync() here: GlanceAppWidgetReceiver already uses it internally.
    private fun refresh(context: Context) {
        val entryPoint = ReceiverEntryPoint.get(context)
        entryPoint.applicationScope().launch { entryPoint.widgetUpdater().refresh() }
    }
}
