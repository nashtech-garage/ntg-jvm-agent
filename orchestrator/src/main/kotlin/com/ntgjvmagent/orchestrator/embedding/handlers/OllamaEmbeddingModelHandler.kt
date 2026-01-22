package com.ntgjvmagent.orchestrator.embedding.handlers

import com.ntgjvmagent.orchestrator.config.LlmProvidersProperties
import com.ntgjvmagent.orchestrator.embedding.config.EmbeddingModelConfig
import com.ntgjvmagent.orchestrator.model.ProviderType
import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.ollama.OllamaEmbeddingModel
import org.springframework.ai.ollama.api.OllamaApi
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions
import org.springframework.ai.ollama.management.ModelManagementOptions
import org.springframework.stereotype.Service
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestClient
import org.springframework.web.reactive.function.client.WebClient

/**
 * Ollama Embedding Model Handler
 * Creates Ollama EmbeddingModel.
 * baseUrl: http://localhost:11434 (default Ollama server)
 * embeddingModel: nomic-embed-text, mxbai-embed-large, etc.
 */
@Service
class OllamaEmbeddingModelHandler(
    private val providerProps: LlmProvidersProperties,
    private val observationRegistry: ObservationRegistry,
) : EmbeddingModelHandler {
    override fun supports(providerType: ProviderType) = providerType == ProviderType.OLLAMA

    override fun createEmbeddingModel(config: EmbeddingModelConfig): EmbeddingModel {
        val provider =
            checkNotNull(providerProps.providers[ProviderType.OLLAMA]) {
                "Provider configuration for OLLAMA is missing under llm.providers"
            }

        val ollamaApi =
            OllamaApi
                .builder()
                .baseUrl(provider.baseUrl) // e.g., "http://localhost:11434"
                .restClientBuilder(RestClient.builder())
                .webClientBuilder(WebClient.builder())
                .responseErrorHandler(DefaultResponseErrorHandler())
                .build()

        val options =
            OllamaEmbeddingOptions
                .builder()
                .model(config.model)
                .build()

        return OllamaEmbeddingModel(
            ollamaApi,
            options,
            observationRegistry,
            ModelManagementOptions.builder().build(),
        )
    }
}
