package com.wakita181009.casdd.infrastructure.adapter

import com.wakita181009.casdd.application.port.ClockPort
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

@Component
class ClockAdapter(private val clock: Clock = Clock.systemUTC()) : ClockPort {
    override fun now(): Instant = Instant.now(clock)
    override fun today(): LocalDate = LocalDate.now(clock)
}
