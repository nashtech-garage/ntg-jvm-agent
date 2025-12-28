package com.ntgjvmagent.orchestrator.usage.query.dto.response.agent

import java.util.UUID

data class UsageByAgentRowDto(
    val agentId: UUID,
    val agentName: String,
    val promptTokens: Long,
    val completionTokens: Long,
    val totalTokens: Long,
)
