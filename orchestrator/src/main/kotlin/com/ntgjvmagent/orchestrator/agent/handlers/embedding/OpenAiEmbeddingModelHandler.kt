package com.ntgjvmagent.orchestrator.agent.handlers.embedding

import com.ntgjvmagent.orchestrator.component.SimpleApiKey
import com.ntgjvmagent.orchestrator.enum.ProviderType
import com.ntgjvmagent.orchestrator.model.EmbeddingModelConfig
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
    private val noRetryTemplate: RetryTemplate,
    private val observationRegistry: ObservationRegistry,
) : EmbeddingModelHandler {
    override fun supports(providerType: ProviderType) = providerType == ProviderType.OPENAI

    override fun createEmbeddingModel(config: EmbeddingModelConfig): EmbeddingModel {
        val api =
            OpenAiApi(
                config.baseUrl,
                SimpleApiKey(config.apiKey),
                HttpHeaders(),
                config.chatCompletionsPath,
                config.embeddingsPath,
                RestClient.builder(),
                WebClient.builder(),
                DefaultResponseErrorHandler(),
            )

        val options =
            OpenAiEmbeddingOptions
                .builder()
                .model(config.embeddingModel)
                .dimensions(config.dimension)
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
