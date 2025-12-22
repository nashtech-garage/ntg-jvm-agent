package com.ntgjvmagent.orchestrator.token

data class TokenBudget(
    val limit: Long,
    val used: Long,
) {
    val remaining: Long get() = limit - used
}
