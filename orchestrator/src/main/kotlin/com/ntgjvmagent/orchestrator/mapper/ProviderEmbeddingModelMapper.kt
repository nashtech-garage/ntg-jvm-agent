package com.ntgjvmagent.orchestrator.mapper

import com.ntgjvmagent.orchestrator.dto.response.ProviderEmbeddingModelDto
import com.ntgjvmagent.orchestrator.entity.provider.ProviderEmbeddingModel

object ProviderEmbeddingModelMapper {
    fun toDto(model: ProviderEmbeddingModel): ProviderEmbeddingModelDto =
        ProviderEmbeddingModelDto(
            id = model.id!!,
            embeddingName = model.embeddingName,
            dimension = model.dimension,
            settings = model.settings,
            active = model.active,
        )
}
