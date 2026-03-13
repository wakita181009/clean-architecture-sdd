package com.wakita181009.casdd.application.port

import java.time.Instant
import java.time.LocalDate

interface ClockPort {
    fun now(): Instant
    fun today(): LocalDate
}
