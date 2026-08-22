package com.ntgjvmagent.orchestrator.chat.handlers

import com.ntgjvmagent.orchestrator.model.ChatModelConfig
import com.ntgjvmagent.orchestrator.model.ProviderType
import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.stereotype.Service

/**
 * OpenAI ChatModel Handler
 * Creates OpenAI ChatModel with OpenAiChatOptions.
 * baseUrl: https://api.openai.com/v1
 * model: gpt-4o, gpt-4-turbo, gpt-3.5-turbo
 */
@Service
class OpenAiChatModelHandler(
    private val observationRegistry: ObservationRegistry,
) : ChatModelHandler {
    override fun supports(providerType: ProviderType): Boolean = providerType == ProviderType.OPENAI

    override fun createChatModel(config: ChatModelConfig): ChatModel {
        val optionsBuilder =
            OpenAiChatOptions
                .builder()
                .baseUrl(
                    OpenAiEndpoint.serviceBaseUrl(
                        config.baseUrl,
                        config.chatCompletionsPath,
                        "/chat/completions",
                    ),
                ).apiKey(config.apiKey)
                .maxRetries(0)
                .model(config.modelName)

        config.temperature?.let { optionsBuilder.temperature(it.toDouble()) }
        config.topP?.let { optionsBuilder.topP(it.toDouble()) }
        config.maxTokens?.let { optionsBuilder.maxTokens(it) }
        config.frequencyPenalty?.let { optionsBuilder.frequencyPenalty(it.toDouble()) }
        config.presencePenalty?.let { optionsBuilder.presencePenalty(it.toDouble()) }

        return OpenAiChatModel
            .builder()
            .options(optionsBuilder.build())
            .observationRegistry(observationRegistry)
            .build()
    }
}
