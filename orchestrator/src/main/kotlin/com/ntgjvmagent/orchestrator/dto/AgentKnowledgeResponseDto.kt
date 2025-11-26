package com.ntgjvmagent.orchestrator.dto

import java.time.OffsetDateTime
import java.util.UUID

data class AgentKnowledgeResponseDto(
    val id: UUID,
    val agentId: UUID,
    val name: String,
    val sourceType: String?,
    val sourceUri: String?,
    val metadata: Map<String, Any>?,
    val embeddingModel: String?,
    val active: Boolean,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime?,
)
