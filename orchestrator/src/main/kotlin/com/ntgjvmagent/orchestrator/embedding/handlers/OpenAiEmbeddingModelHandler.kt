package com.ntgjvmagent.orchestrator.embedding.handlers

import com.ntgjvmagent.orchestrator.component.SimpleApiKey
import com.ntgjvmagent.orchestrator.config.LlmProvidersProperties
import com.ntgjvmagent.orchestrator.embedding.config.EmbeddingModelConfig
import com.ntgjvmagent.orchestrator.model.ProviderType
import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.document.MetadataMode
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.openai.OpenAiEmbeddingModel
import org.springframework.ai.openai.OpenAiEmbeddingOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.core.retry.RetryTemplate
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestClient
import org.springframework.web.reactive.function.client.WebClient

@Service
class OpenAiEmbeddingModelHandler(
    private val providerProps: LlmProvidersProperties,
    private val noRetryTemplate: RetryTemplate,
    private val observationRegistry: ObservationRegistry,
) : EmbeddingModelHandler {
    override fun supports(providerType: ProviderType): Boolean = providerType == ProviderType.OPENAI

    override fun createEmbeddingModel(config: EmbeddingModelConfig): EmbeddingModel {
        val provider =
            checkNotNull(providerProps.providers[ProviderType.OPENAI]) {
                "Provider configuration for OPENAI is missing under llm.providers"
            }

        val api =
            OpenAiApi(
                provider.baseUrl,
                SimpleApiKey(provider.apiKey),
                HttpHeaders(),
                provider.completionsPath, // chatCompletionsPath (not used for embeddings)
                provider.embeddingsPath, // embeddings endpoint
                RestClient.builder(),
                WebClient.builder(),
                DefaultResponseErrorHandler(),
            )

        val options =
            OpenAiEmbeddingOptions
                .builder()
                .model(config.model)
                .build()

        return OpenAiEmbeddingModel(
            api,
            MetadataMode.EMBED,
            options,
            noRetryTemplate,
            observationRegistry,
        )
    }
}
