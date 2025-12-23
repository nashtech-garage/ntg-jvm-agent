package com.ntgjvmagent.orchestrator.token.accounting

data class TokenBudget(
    val limit: Long,
    val used: Long,
) {
    val remaining: Long get() = limit - used
}
