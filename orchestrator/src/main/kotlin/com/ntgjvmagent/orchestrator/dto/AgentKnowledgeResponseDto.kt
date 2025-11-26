package com.ntgjvmagent.orchestrator.dto

import java.time.Instant
import java.util.UUID

data class AgentKnowledgeResponseDto(
    val id: UUID,
    val agentId: UUID,
    val name: String,
    val sourceType: String?,
    val sourceUri: String?,
    val metadata: Map<String, Any>?,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant?,
)
