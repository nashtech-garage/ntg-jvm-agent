package com.ntgjvmagent.orchestrator.embedding.orchestrator

import com.ntgjvmagent.orchestrator.embedding.config.EmbeddingModelConfig
import com.ntgjvmagent.orchestrator.embedding.handlers.EmbeddingModelHandler
import org.slf4j.LoggerFactory
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.stereotype.Service

@Service
class EmbeddingModelOrchestrator(
    private val handlers: List<EmbeddingModelHandler>,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun createEmbeddingModel(config: EmbeddingModelConfig): EmbeddingModel {
        logger.info(
            "EmbeddingModelConfig → providerType={}, model={}",
            config.providerType,
            config.model,
        )

        val handler =
            handlers.find { it.supports(config.providerType) }
                ?: error("No embedding handler for provider: ${config.providerType}")

        logger.info("Selected handler = {}", handler::class.simpleName)

        return handler.createEmbeddingModel(config)
    }
}
