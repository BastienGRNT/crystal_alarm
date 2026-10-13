package com.crystal.alarm.platform.time

import com.crystal.alarm.domain.port.TimeSource
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

class SystemTimeSource @Inject constructor() : TimeSource {
    override fun now(): Instant = Instant.now()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}
