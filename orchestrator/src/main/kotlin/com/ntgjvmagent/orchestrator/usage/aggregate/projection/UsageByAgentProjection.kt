package com.ntgjvmagent.orchestrator.usage.aggregate.projection

import java.util.UUID

interface UsageByAgentProjection {
    val agentId: UUID
    val agentName: String
    val promptTokens: Long
    val completionTokens: Long
    val totalTokens: Long
}
