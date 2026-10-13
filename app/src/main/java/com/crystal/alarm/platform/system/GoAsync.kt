package com.crystal.alarm.platform.system

import android.content.BroadcastReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Runs [block] off the main thread while keeping the receiver alive until it completes. */
fun BroadcastReceiver.launchAsync(scope: CoroutineScope, block: suspend () -> Unit) {
    val pending = goAsync()
    scope.launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}
