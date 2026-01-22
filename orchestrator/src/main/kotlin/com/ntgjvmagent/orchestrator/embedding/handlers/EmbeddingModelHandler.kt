package com.ntgjvmagent.orchestrator.embedding.handlers

import com.ntgjvmagent.orchestrator.embedding.config.EmbeddingModelConfig
import com.ntgjvmagent.orchestrator.model.ProviderType
import org.springframework.ai.embedding.EmbeddingModel

interface EmbeddingModelHandler {
    fun supports(providerType: ProviderType): Boolean

    fun createEmbeddingModel(config: EmbeddingModelConfig): EmbeddingModel
}
