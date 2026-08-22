package com.ntgjvmagent.orchestrator.embedding.handlers

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
class AzureOpenAiEmbeddingModelHandler(
    private val providerProps: LlmProvidersProperties,
    private val observationRegistry: ObservationRegistry,
) : EmbeddingModelHandler {
    override fun supports(providerType: ProviderType): Boolean = providerType == ProviderType.AZURE_OPENAI

    override fun createEmbeddingModel(config: EmbeddingModelConfig): EmbeddingModel {
        val provider =
            checkNotNull(providerProps.providers[ProviderType.AZURE_OPENAI]) {
                "Provider configuration for AZURE_OPENAI is missing under llm.providers"
            }

        val options =
            OpenAiEmbeddingOptions
                .builder()
                .baseUrl(provider.baseUrl)
                .apiKey(provider.apiKey)
                .azure(true)
                .deploymentName(config.model)
                .model(config.model)
                .maxRetries(0)
                .build()

        return OpenAiEmbeddingModel
            .builder()
            .metadataMode(MetadataMode.EMBED)
            .options(options)
            .observationRegistry(observationRegistry)
            .build()
    }
}
