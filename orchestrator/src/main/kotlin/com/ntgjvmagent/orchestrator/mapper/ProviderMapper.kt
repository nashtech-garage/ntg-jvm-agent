package com.ntgjvmagent.orchestrator.mapper

import com.ntgjvmagent.orchestrator.dto.response.ProviderResponseDto
import com.ntgjvmagent.orchestrator.entity.provider.Provider

object ProviderMapper {
    fun toProviderResponseDto(provider: Provider): ProviderResponseDto =
        ProviderResponseDto(
            id = provider.id!!,
            name = provider.name,
            description = provider.description,
            active = provider.active,
            baseUrl = provider.baseUrl,
            chatCompletionsPath = provider.chatCompletionsPath,
            embeddingsPath = provider.embeddingsPath,
            settings = provider.settings,
            models =
                provider.models
                    .map { ProviderModelMapper.toDto(it) }
                    .toSet(),
            embeddingModels =
                provider.embeddingModels
                    .map { ProviderEmbeddingModelMapper.toDto(it) }
                    .toSet(),
        )
}
