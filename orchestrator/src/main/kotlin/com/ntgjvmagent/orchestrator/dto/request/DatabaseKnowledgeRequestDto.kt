package com.ntgjvmagent.orchestrator.dto.request

import com.ntgjvmagent.orchestrator.model.KnowledgeSourceType
import jakarta.validation.constraints.NotBlank

data class DatabaseKnowledgeRequestDto(
    @field:NotBlank
    val dbHost: String,
    @field:NotBlank
    val dbPort: String,
    @field:NotBlank
    val dbUser: String,
    @field:NotBlank
    val dbPassword: String,
    @field:NotBlank
    val dbQuery: String,
) : AgentKnowledgeRequestDto {
    override val sourceType = KnowledgeSourceType.DATABASE
    override val sourceUri: String? = null
    override val metadata: Map<String, Any?> =
        mapOf(
            "host" to dbHost,
            "port" to dbPort,
            "user" to dbUser,
            "password" to dbPassword,
            "query" to dbQuery,
        )
}
