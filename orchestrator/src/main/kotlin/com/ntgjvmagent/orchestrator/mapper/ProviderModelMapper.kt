package com.ntgjvmagent.orchestrator.mapper

import com.ntgjvmagent.orchestrator.dto.response.ProviderModelDto
import com.ntgjvmagent.orchestrator.entity.provider.ProviderModel

object ProviderModelMapper {
    fun toDto(model: ProviderModel): ProviderModelDto =
        ProviderModelDto(
            id = model.id!!,
            modelName = model.modelName,
            contextWindow = model.contextWindow,
            defaultTemperature = model.defaultTemperature.toDouble(),
            defaultTopP = model.defaultTopP.toDouble(),
            defaultMaxTokens = model.defaultMaxTokens,
            defaultFrequencyPenalty = model.defaultFrequencyPenalty.toDouble(),
            defaultPresencePenalty = model.defaultPresencePenalty.toDouble(),
            defaultDimension = model.defaultDimension,
            settings = model.defaultSettings,
            active = model.active,
        )
}
