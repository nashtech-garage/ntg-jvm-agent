package com.ntgjvmagent.orchestrator.dto

import java.time.OffsetDateTime
import java.util.UUID

data class AgentToolResponseDto(
    val id: UUID,
    val agentId: UUID,
    val toolId: UUID,
    val config: Map<String, Any>? = null,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime?,
)
