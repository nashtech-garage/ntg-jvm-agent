package com.ntgjvmagent.orchestrator.dto.response

import java.util.UUID

data class AgentResponseDto(
    val id: UUID,
    val name: String,
    val description: String?,
    val avatar: String?,
    val active: Boolean,
    val provider: String,
    val baseUrl: String,
    val apiKey: String,
    val chatCompletionsPath: String,
    val model: String,
    val embeddingModel: String,
    val dimension: Int,
    val embeddingsPath: String,
    val topP: Double,
    val temperature: Double,
    val maxTokens: Int,
    val frequencyPenalty: Double,
    val presencePenalty: Double,
    val settings: Map<String, Any>?,
)
