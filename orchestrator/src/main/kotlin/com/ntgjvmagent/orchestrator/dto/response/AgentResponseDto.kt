package com.ntgjvmagent.orchestrator.dto.response

import com.ntgjvmagent.orchestrator.model.ProviderType
import java.util.UUID

data class AgentResponseDto(
    val id: UUID,
    val name: String,
    val description: String?,
    val avatar: String?,
    val active: Boolean,
    val provider: ProviderType,
    val baseUrl: String,
    val apiKey: String,
    val chatCompletionsPath: String,
    val model: String,
    val topP: Double,
    val temperature: Double,
    val maxTokens: Int,
    val frequencyPenalty: Double,
    val presencePenalty: Double,
    val settings: Map<String, Any>?,
)
