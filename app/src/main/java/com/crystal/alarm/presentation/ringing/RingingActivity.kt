package com.crystal.alarm.presentation.ringing

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.crystal.alarm.domain.model.AlarmState
import com.crystal.alarm.domain.repository.AlarmRepository
import com.crystal.alarm.platform.alarm.RingingScreenVisibility
import com.crystal.alarm.presentation.theme.CrystalTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Shown over the lock screen when the alarm rings. Back is swallowed; the only ways out
 * are scanning the wake code or completing the rescue challenge. Leaving it any other way
 * brings it back (see [com.crystal.alarm.platform.alarm.RingingScreenKeeper]).
 */
@AndroidEntryPoint
class RingingActivity : ComponentActivity() {

    @Inject lateinit var alarmRepository: AlarmRepository

    private val viewModel: RingingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(this) { viewModel.onBack() }

        setContent {
            CrystalTheme {
                RingingScreen(viewModel)
            }
        }

        lifecycleScope.launch {
            // The notification can open us a few ms before the service marks the alarm as ringing.
            val ringing = withTimeoutOrNull(STARTUP_GRACE_MS) {
                alarmRepository.state.first { it is AlarmState.Ringing }
            }
            if (ringing != null) alarmRepository.state.first { it !is AlarmState.Ringing }
            finishAndRemoveTask()
        }
    }

    override fun onStart() {
        super.onStart()
        RingingScreenVisibility.onShown()
    }

    override fun onStop() {
        RingingScreenVisibility.onHidden()
        super.onStop()
    }

    private companion object {
        const val STARTUP_GRACE_MS = 5_000L
    }
}
