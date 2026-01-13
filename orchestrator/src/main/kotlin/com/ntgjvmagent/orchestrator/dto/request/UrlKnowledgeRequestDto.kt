package com.ntgjvmagent.orchestrator.dto.request

import com.ntgjvmagent.orchestrator.model.KnowledgeSourceType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class UrlKnowledgeRequestDto(
    @field:NotBlank
    @field:Pattern(regexp = "https?://.*", message = "Must be a valid URL")
    val url: String,
) : AgentKnowledgeRequestDto {
    override val sourceType = KnowledgeSourceType.WEB_URL
    override val sourceUri: String = url
    override val metadata: Map<String, Any?> = mapOf("url" to url)
}
