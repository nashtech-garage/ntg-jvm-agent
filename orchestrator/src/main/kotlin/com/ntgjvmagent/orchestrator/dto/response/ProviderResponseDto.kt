package com.ntgjvmagent.orchestrator.dto.response

import java.util.UUID

data class ProviderResponseDto(
    val id: UUID,
    val name: String,
    val description: String?,
    val active: Boolean,
    val baseUrl: String,
    val chatCompletionsPath: String,
    val embeddingsPath: String,
    val settings: Map<String, Any>?,
    val models: Set<ProviderModelDto> = mutableSetOf(),
    val embeddingModels: Set<ProviderEmbeddingModelDto> = mutableSetOf(),
)

data class ProviderModelDto(
    val id: UUID,
    val modelName: String,
    val contextWindow: Int?,
    val defaultTemperature: Double,
    val defaultTopP: Double,
    val defaultMaxTokens: Int,
    val defaultFrequencyPenalty: Double,
    val defaultPresencePenalty: Double,
    val defaultDimension: Int,
    val settings: Map<String, Any>?,
    val active: Boolean,
)

data class ProviderEmbeddingModelDto(
    val id: UUID,
    val embeddingName: String,
    val dimension: Int,
    val settings: Map<String, Any>?,
    val active: Boolean,
)
