package com.ntgjvmagent.orchestrator.dto

import jakarta.validation.constraints.NotBlank

data class KnowledgeChunkRequestDto(
    @field:NotBlank val content: String,
    val metadata: Map<String, Any>? = null,
)
