package com.ntgjvmagent.orchestrator.agent

import com.ntgjvmagent.orchestrator.agent.handlers.embedding.EmbeddingModelHandler
import com.ntgjvmagent.orchestrator.model.EmbeddingModelConfig
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.stereotype.Service

@Service
class EmbeddingModelOrchestrator(
    private val handlers: List<EmbeddingModelHandler>,
) {
    fun createEmbeddingModel(config: EmbeddingModelConfig): EmbeddingModel {
        val handler =
            handlers.find { it.supports(config.providerType) }
                ?: throw IllegalArgumentException("No embedding handler for provider: ${config.providerType}")

        return handler.createEmbeddingModel(config)
    }
}
