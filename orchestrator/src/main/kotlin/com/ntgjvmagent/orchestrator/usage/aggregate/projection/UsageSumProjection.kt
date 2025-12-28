package com.ntgjvmagent.orchestrator.usage.aggregate.projection

interface UsageSumProjection {
    val totalTokens: Long
    val promptTokens: Long
    val completionTokens: Long
}
