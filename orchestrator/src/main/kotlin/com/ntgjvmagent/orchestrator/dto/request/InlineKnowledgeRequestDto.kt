package com.ntgjvmagent.orchestrator.dto.request

import com.ntgjvmagent.orchestrator.model.KnowledgeSourceType
import jakarta.validation.constraints.NotBlank

data class InlineKnowledgeRequestDto(
    @field:NotBlank
    val inlineContent: String,
) : AgentKnowledgeRequestDto {
    override val sourceType = KnowledgeSourceType.INLINE
    override val sourceUri: String? = null
    override val metadata = mapOf("content" to inlineContent)
}
