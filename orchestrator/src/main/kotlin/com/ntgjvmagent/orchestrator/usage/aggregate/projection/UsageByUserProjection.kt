package com.ntgjvmagent.orchestrator.usage.aggregate.projection

import java.util.UUID

interface UsageByUserProjection {
    val userId: UUID?
    val userName: String?
    val promptTokens: Long
    val completionTokens: Long
    val totalTokens: Long
}
