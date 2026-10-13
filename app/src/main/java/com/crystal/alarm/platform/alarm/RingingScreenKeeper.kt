package com.crystal.alarm.platform.alarm

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import androidx.core.content.getSystemService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Brings the ringing screen back whenever the user leaves it (home button, another app,
 * screen turned off) until the alarm is stopped. Leaving the screen must never be a way
 * to silence the morning.
 *
 * Starting an activity from the background needs the "display over other apps" permission.
 * While ringing, a 1 px invisible overlay window is also kept on screen: recent Android
 * versions only honor that permission when the app shows an overlay.
 */
class RingingScreenKeeper(private val context: Context) {

    private var anchor: View? = null

    /** Runs until cancelled; the service cancels it as soon as the alarm leaves the ringing state. */
    suspend fun keepInFront() {
        try {
            addAnchor()
            while (true) {
                delay(CHECK_INTERVAL_MS)
                if (!RingingScreenVisibility.visible.value) {
                    context.startActivity(AlarmNotifications.ringingScreenIntent(context))
                }
            }
        } finally {
            removeAnchor()
        }
    }

    private fun addAnchor() {
        if (!Settings.canDrawOverlays(context)) return
        val windowManager = context.getSystemService<WindowManager>() ?: return
        val view = View(context)
        val params = WindowManager.LayoutParams(
            1,
            1,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        )
        // Permission revoked between the check and the call: ringing must go on without it.
        runCatching { windowManager.addView(view, params) }.onSuccess { anchor = view }
    }

    private fun removeAnchor() {
        val view = anchor ?: return
        anchor = null
        runCatching { context.getSystemService<WindowManager>()?.removeView(view) }
    }

    private companion object {
        /** Short enough to feel immediate, long enough not to fight the activity's own transitions. */
        const val CHECK_INTERVAL_MS = 1_500L
    }
}

/** Whether [com.crystal.alarm.presentation.ringing.RingingActivity] is currently on screen. */
object RingingScreenVisibility {
    private val _visible = MutableStateFlow(false)
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    fun onShown() {
        _visible.value = true
    }

    fun onHidden() {
        _visible.value = false
    }
}
