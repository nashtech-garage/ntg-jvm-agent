package com.ntgjvmagent.orchestrator.embedding.handlers

import com.azure.ai.openai.OpenAIClientBuilder
import com.azure.core.credential.AzureKeyCredential
import com.azure.core.http.policy.FixedDelayOptions
import com.azure.core.http.policy.RetryOptions
import com.ntgjvmagent.orchestrator.config.LlmProvidersProperties
import com.ntgjvmagent.orchestrator.embedding.config.EmbeddingModelConfig
import com.ntgjvmagent.orchestrator.model.ProviderType
import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.azure.openai.AzureOpenAiEmbeddingModel
import org.springframework.ai.azure.openai.AzureOpenAiEmbeddingOptions
import org.springframework.ai.document.MetadataMode
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.stereotype.Service
import java.time.Duration

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

        // Disable retries explicitly
        val retryOptions =
            RetryOptions(
                FixedDelayOptions(
                    0,
                    Duration.ZERO,
                ),
            ).setShouldRetryCondition { false }

        val client =
            OpenAIClientBuilder()
                .credential(AzureKeyCredential(provider.apiKey))
                .endpoint(provider.baseUrl)
                .retryOptions(retryOptions)
                .buildClient()

        val options =
            AzureOpenAiEmbeddingOptions
                .builder()
                // Azure OpenAI uses DEPLOYMENT NAME, not model family
                .deploymentName(config.model)
                .build()

        return AzureOpenAiEmbeddingModel(
            client,
            MetadataMode.EMBED,
            options,
            observationRegistry,
        )
    }
}
