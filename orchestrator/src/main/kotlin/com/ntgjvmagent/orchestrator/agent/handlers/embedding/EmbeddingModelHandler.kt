package com.ntgjvmagent.orchestrator.agent.handlers.embedding

import com.ntgjvmagent.orchestrator.enum.ProviderType
import com.ntgjvmagent.orchestrator.model.EmbeddingModelConfig
import org.springframework.ai.embedding.EmbeddingModel

interface EmbeddingModelHandler {
    fun supports(providerType: ProviderType): Boolean
    fun createEmbeddingModel(config: EmbeddingModelConfig): EmbeddingModel
}
