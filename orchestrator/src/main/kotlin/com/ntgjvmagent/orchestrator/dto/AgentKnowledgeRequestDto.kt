package com.ntgjvmagent.orchestrator.dto

import jakarta.validation.constraints.NotBlank

data class AgentKnowledgeRequestDto(
    @field:NotBlank val name: String,
    val sourceType: String? = null,
    val sourceUri: String? = null,
    val metadata: Map<String, Any>? = null,
    val embeddingModel: String? = null,
    val active: Boolean = true,
)
