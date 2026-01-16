package com.ntgjvmagent.orchestrator.agent.handlers.embedding

import com.azure.ai.openai.OpenAIClientBuilder
import com.azure.core.credential.AzureKeyCredential
import com.azure.core.http.policy.FixedDelayOptions
import com.azure.core.http.policy.RetryOptions
import com.ntgjvmagent.orchestrator.enum.ProviderType
import com.ntgjvmagent.orchestrator.model.EmbeddingModelConfig
import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.azure.openai.AzureOpenAiEmbeddingModel
import org.springframework.ai.azure.openai.AzureOpenAiEmbeddingOptions
import org.springframework.ai.document.MetadataMode
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class AzureOpenAiEmbeddingModelHandler(
    private val observationRegistry: ObservationRegistry,
) : EmbeddingModelHandler {
    override fun supports(providerType: ProviderType) = providerType == ProviderType.AZURE_OPENAI

    override fun createEmbeddingModel(config: EmbeddingModelConfig): EmbeddingModel {
        val retryOptions =
            RetryOptions(FixedDelayOptions(0, Duration.ZERO))
                .setShouldRetryCondition { false }

        val client =
            OpenAIClientBuilder()
                .credential(AzureKeyCredential(config.apiKey))
                .endpoint(config.baseUrl)
                .retryOptions(retryOptions)
                .buildClient()

        val options =
            AzureOpenAiEmbeddingOptions
                .builder()
                .deploymentName(config.embeddingModel) // Azure uses deploymentName
                .dimensions(config.dimension)
                .build()

        return AzureOpenAiEmbeddingModel(
            client,
            MetadataMode.EMBED,
            options,
            observationRegistry,
        )
    }
}
