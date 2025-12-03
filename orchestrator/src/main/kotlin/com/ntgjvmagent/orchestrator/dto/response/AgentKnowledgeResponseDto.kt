package com.ntgjvmagent.orchestrator.dto.response

import com.ntgjvmagent.orchestrator.model.KnowledgeSourceType
import java.time.Instant
import java.util.UUID

data class AgentKnowledgeResponseDto(
    val id: UUID,
    val agentId: UUID,
    val name: String,
    val sourceType: KnowledgeSourceType,
    val sourceUri: String?,
    val metadata: Map<String, Any?>,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant?,
)
