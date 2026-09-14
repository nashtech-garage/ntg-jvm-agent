package com.ntgjvmagent.orchestrator.dto.response

import java.time.Instant
import java.util.UUID

data class AgentMemoryResponseDto(
    val id: UUID,
    val agentId: UUID?,
    val type: String,
    val name: String,
    val description: String,
    val content: String,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)
