package com.crystal.alarm.presentation.components

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Emits immediately, then every [periodMillis]. Drives countdowns. */
fun tickerFlow(periodMillis: Long = 1_000): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        delay(periodMillis)
    }
}
