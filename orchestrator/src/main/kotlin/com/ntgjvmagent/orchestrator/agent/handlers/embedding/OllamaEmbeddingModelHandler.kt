package com.ntgjvmagent.orchestrator.agent.handlers.embedding

import com.ntgjvmagent.orchestrator.enum.ProviderType
import com.ntgjvmagent.orchestrator.model.EmbeddingModelConfig
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
    private val observationRegistry: ObservationRegistry,
) : EmbeddingModelHandler {
    override fun supports(providerType: ProviderType) = providerType == ProviderType.OLLAMA

    override fun createEmbeddingModel(config: EmbeddingModelConfig): EmbeddingModel {
        val ollamaApi =
            OllamaApi
                .builder()
                .baseUrl(config.baseUrl) // e.g., "http://localhost:11434"
                .restClientBuilder(RestClient.builder())
                .webClientBuilder(WebClient.builder())
                .responseErrorHandler(DefaultResponseErrorHandler())
                .build()

        val options =
            OllamaEmbeddingOptions
                .builder()
                .model(config.embeddingModel)
                .build()

        return OllamaEmbeddingModel(
            ollamaApi,
            options,
            observationRegistry,
            ModelManagementOptions.builder().build(),
        )
    }
}
