package com.ntgjvmagent.orchestrator.chat.handlers

import com.azure.ai.openai.OpenAIClientBuilder
import com.azure.core.credential.AzureKeyCredential
import com.azure.core.http.policy.FixedDelayOptions
import com.azure.core.http.policy.RetryOptions
import com.ntgjvmagent.orchestrator.model.ChatModelConfig
import com.ntgjvmagent.orchestrator.model.ProviderType
import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.azure.openai.AzureOpenAiChatModel
import org.springframework.ai.azure.openai.AzureOpenAiChatOptions
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.model.tool.ToolCallingManager
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * Azure OpenAI ChatModel Handler
 * Creates Azure OpenAI ChatModel using native AzureOpenAiChatModel.
 * baseUrl: https://resource.openai.azure.com/
 * modelName: deployment name (e.g., gpt-4o)
 */
@Service
class AzureOpenAiChatModelHandler(
    private val toolCallingManager: ToolCallingManager,
    private val observationRegistry: ObservationRegistry,
) : ChatModelHandler {
    override fun supports(providerType: ProviderType): Boolean = providerType == ProviderType.AZURE_OPENAI

    override fun createChatModel(config: ChatModelConfig): ChatModel {
        val clientBuilder =
            OpenAIClientBuilder()
                .credential(AzureKeyCredential(config.apiKey))
                .endpoint(config.baseUrl)
                .retryOptions(
                    RetryOptions(
                        FixedDelayOptions(
                            0, // no retries
                            Duration.ZERO,
                        ),
                    ).setShouldRetryCondition { false },
                )

        val optionsBuilder =
            AzureOpenAiChatOptions
                .builder()
                // IMPORTANT: Azure uses deployment name, not model family
                .deploymentName(config.modelName)

        // Apply agent behavior ONLY if explicitly configured
        config.temperature?.let { optionsBuilder.temperature(it.toDouble()) }
        config.topP?.let { optionsBuilder.topP(it.toDouble()) }
        config.frequencyPenalty?.let { optionsBuilder.frequencyPenalty(it.toDouble()) }
        config.presencePenalty?.let { optionsBuilder.presencePenalty(it.toDouble()) }
        config.maxTokens?.let { optionsBuilder.maxTokens(it) }

        return AzureOpenAiChatModel
            .builder()
            .openAIClientBuilder(clientBuilder)
            .defaultOptions(optionsBuilder.build())
            .toolCallingManager(toolCallingManager)
            .observationRegistry(observationRegistry)
            .build()
    }
}
