package com.ntgjvmagent.orchestrator.embedding.handlers

import com.ntgjvmagent.orchestrator.chat.handlers.OpenAiEndpoint
import com.ntgjvmagent.orchestrator.config.LlmProvidersProperties
import com.ntgjvmagent.orchestrator.embedding.config.EmbeddingModelConfig
import com.ntgjvmagent.orchestrator.model.ProviderType
import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.document.MetadataMode
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.openai.OpenAiEmbeddingModel
import org.springframework.ai.openai.OpenAiEmbeddingOptions
import org.springframework.stereotype.Service

@Service
class OpenAiEmbeddingModelHandler(
    private val providerProps: LlmProvidersProperties,
    private val observationRegistry: ObservationRegistry,
) : EmbeddingModelHandler {
    override fun supports(providerType: ProviderType): Boolean = providerType == ProviderType.OPENAI

    override fun createEmbeddingModel(config: EmbeddingModelConfig): EmbeddingModel {
        val provider =
            checkNotNull(providerProps.providers[ProviderType.OPENAI]) {
                "Provider configuration for OPENAI is missing under llm.providers"
            }

        val options =
            OpenAiEmbeddingOptions
                .builder()
                .baseUrl(
                    OpenAiEndpoint.serviceBaseUrl(
                        provider.baseUrl,
                        provider.embeddingsPath,
                        "/embeddings",
                    ),
                ).apiKey(provider.apiKey)
                .maxRetries(0)
                .model(config.model)
                .build()

        return OpenAiEmbeddingModel
            .builder()
            .metadataMode(MetadataMode.EMBED)
            .options(options)
            .observationRegistry(observationRegistry)
            .build()
    }
}
