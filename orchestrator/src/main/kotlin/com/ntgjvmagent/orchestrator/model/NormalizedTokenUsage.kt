package com.ntgjvmagent.orchestrator.model

data class NormalizedTokenUsage(
    val promptTokens: Long,
    val completionTokens: Long,
) {
    val totalTokens: Long
        get() = promptTokens + completionTokens
}
