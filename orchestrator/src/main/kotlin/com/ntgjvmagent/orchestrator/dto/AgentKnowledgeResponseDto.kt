package com.ntgjvmagent.orchestrator.dto

import java.util.UUID

data class AgentKnowledgeResponseDto(
    val id: UUID,
    val name: String,
    val sourceType: String?,
    val sourceUri: String?,
    val metadata: Map<String, Any>?,
    val embeddingModel: String?,
    val active: Boolean,
)
