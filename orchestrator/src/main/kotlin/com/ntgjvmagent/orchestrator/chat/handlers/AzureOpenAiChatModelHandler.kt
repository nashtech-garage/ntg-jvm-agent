package com.ntgjvmagent.orchestrator.chat.handlers

import com.ntgjvmagent.orchestrator.model.ChatModelConfig
import com.ntgjvmagent.orchestrator.model.ProviderType
import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.stereotype.Service

/**
 * Azure OpenAI ChatModel Handler
 * Creates an OpenAI ChatModel configured for Azure OpenAI.
 * baseUrl: https://resource.openai.azure.com/
 * modelName: deployment name (e.g., gpt-4o)
 */
@Service
class AzureOpenAiChatModelHandler(
    private val observationRegistry: ObservationRegistry,
) : ChatModelHandler {
    override fun supports(providerType: ProviderType): Boolean = providerType == ProviderType.AZURE_OPENAI

    override fun createChatModel(config: ChatModelConfig): ChatModel {
        val optionsBuilder =
            OpenAiChatOptions
                .builder()
                .baseUrl(config.baseUrl)
                .apiKey(config.apiKey)
                .azure(true)
                .deploymentName(config.modelName)
                .model(config.modelName)
                .maxRetries(0)

        // Apply agent behavior ONLY if explicitly configured
        config.temperature?.let { optionsBuilder.temperature(it.toDouble()) }
        config.topP?.let { optionsBuilder.topP(it.toDouble()) }
        config.frequencyPenalty?.let { optionsBuilder.frequencyPenalty(it.toDouble()) }
        config.presencePenalty?.let { optionsBuilder.presencePenalty(it.toDouble()) }
        config.maxTokens?.let { optionsBuilder.maxCompletionTokens(it) }

        return OpenAiChatModel
            .builder()
            .options(optionsBuilder.build())
            .observationRegistry(observationRegistry)
            .build()
    }
}
