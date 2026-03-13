package com.wakita181009.casdd.infrastructure.adapter

import com.wakita181009.casdd.application.port.TransactionPort
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

@Component
class TransactionAdapter(
    private val transactionTemplate: TransactionTemplate,
) : TransactionPort {
    override fun <T> execute(block: () -> T): T = transactionTemplate.execute { block() }!!
}
