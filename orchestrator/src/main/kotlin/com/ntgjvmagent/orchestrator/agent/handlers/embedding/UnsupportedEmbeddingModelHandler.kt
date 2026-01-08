package com.ntgjvmagent.orchestrator.agent.handlers.embedding

import com.ntgjvmagent.orchestrator.enum.ProviderType
import com.ntgjvmagent.orchestrator.model.EmbeddingModelConfig
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.stereotype.Service

@Service
class UnsupportedEmbeddingModelHandler : EmbeddingModelHandler {
    override fun supports(providerType: ProviderType) =
        providerType == ProviderType.OLLAMA ||
            providerType == ProviderType.BEDROCK ||
            providerType == ProviderType.ANTHROPIC

    override fun createEmbeddingModel(config: EmbeddingModelConfig): EmbeddingModel =
        throw UnsupportedOperationException("${config.providerType} embedding is not yet supported.")
}
