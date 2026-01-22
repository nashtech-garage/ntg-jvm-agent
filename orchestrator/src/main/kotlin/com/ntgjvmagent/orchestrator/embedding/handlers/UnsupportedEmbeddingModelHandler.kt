package com.ntgjvmagent.orchestrator.embedding.handlers

import com.ntgjvmagent.orchestrator.embedding.config.EmbeddingModelConfig
import com.ntgjvmagent.orchestrator.model.ProviderType
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.stereotype.Service

@Service
class UnsupportedEmbeddingModelHandler : EmbeddingModelHandler {
    override fun supports(providerType: ProviderType) =
        providerType == ProviderType.BEDROCK ||
            providerType == ProviderType.ANTHROPIC

    override fun createEmbeddingModel(config: EmbeddingModelConfig): EmbeddingModel =
        throw UnsupportedOperationException("${config.providerType} embedding is not yet supported.")
}
