package com.ntgjvmagent.orchestrator.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.stereotype.Component

@Component
class LlmConfigStartupValidator(
    private val embeddingProps: EmbeddingProperties,
    private val llmProviders: LlmProvidersProperties,
) : SmartInitializingSingleton {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun afterSingletonsInstantiated() {
        validateEmbeddingConfig()
    }

    private fun validateEmbeddingConfig() {
        val providerType = embeddingProps.provider

        val providerConfig =
            llmProviders.providers[providerType]
                ?: throw IllegalStateException(
                    "Embedding provider '$providerType' is not configured under llm.providers. " +
                        "Configured providers: ${llmProviders.providers.keys.joinToString()}",
                )

        if (providerConfig.apiKey.isBlank()) {
            logger.warn(
                "Embedding provider {} has no API key; provider-dependent embedding operations are unavailable",
                providerType,
            )
        }

        require(embeddingProps.model.isNotBlank()) {
            "Embedding model must be set for provider '$providerType'"
        }

        require(embeddingProps.dimension > 0) {
            "Embedding dimension must be > 0 (actual: ${embeddingProps.dimension})"
        }
    }
}
