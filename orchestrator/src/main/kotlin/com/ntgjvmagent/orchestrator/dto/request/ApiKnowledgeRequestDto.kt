package com.ntgjvmagent.orchestrator.dto.request

import com.ntgjvmagent.orchestrator.model.KnowledgeSourceType
import jakarta.validation.constraints.NotBlank

data class ApiKnowledgeRequestDto(
    @field:NotBlank
    override val name: String,
    @field:NotBlank
    val apiUrl: String,
    @field:NotBlank
    val apiMethod: String,
) : AgentKnowledgeRequestDto {
    override val sourceType = KnowledgeSourceType.API
    override val sourceUri: String = apiUrl
    override val metadata =
        mapOf(
            "apiUrl" to apiUrl,
            "method" to apiMethod,
        )
}
