package com.crystal.alarm.domain.model

import java.time.Instant

/** Lifecycle of the single alarm. There is never more than one. */
sealed interface AlarmState {
    data object None : AlarmState

    data class Scheduled(val alarm: Alarm) : AlarmState

    /** The alarm is ringing and can only be stopped by scanning the wake code or completing rescue. */
    data class Ringing(val alarm: Alarm, val since: Instant) : AlarmState
}
