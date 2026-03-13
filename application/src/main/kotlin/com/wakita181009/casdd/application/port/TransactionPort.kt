package com.wakita181009.casdd.application.port

interface TransactionPort {
    fun <T> execute(block: () -> T): T
}
