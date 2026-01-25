package com.ntgjvmagent.orchestrator.chat.handlers

import com.ntgjvmagent.orchestrator.component.SimpleApiKey
import com.ntgjvmagent.orchestrator.model.ChatModelConfig
import com.ntgjvmagent.orchestrator.model.ProviderType
import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.model.tool.ToolCallingManager
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.core.retry.RetryTemplate
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestClient
import org.springframework.web.reactive.function.client.WebClient

/**
 * OpenAI ChatModel Handler
 * Creates OpenAI ChatModel with OpenAiChatOptions.
 * baseUrl: https://api.openai.com/v1
 * model: gpt-4o, gpt-4-turbo, gpt-3.5-turbo
 */
@Service
class OpenAiChatModelHandler(
    private val toolCallingManager: ToolCallingManager,
    private val noRetryTemplate: RetryTemplate,
    private val observationRegistry: ObservationRegistry,
) : ChatModelHandler {
    override fun supports(providerType: ProviderType): Boolean = providerType == ProviderType.OPENAI

    override fun createChatModel(config: ChatModelConfig): ChatModel {
        val openAiApi =
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

        val optionsBuilder =
            OpenAiChatOptions
                .builder()
                .model(config.modelName)

        config.temperature?.let { optionsBuilder.temperature(it.toDouble()) }
        config.topP?.let { optionsBuilder.topP(it.toDouble()) }
        config.maxTokens?.let { optionsBuilder.maxTokens(it) }
        config.frequencyPenalty?.let { optionsBuilder.frequencyPenalty(it.toDouble()) }
        config.presencePenalty?.let { optionsBuilder.presencePenalty(it.toDouble()) }

        return OpenAiChatModel(
            openAiApi,
            optionsBuilder.build(),
            toolCallingManager,
            noRetryTemplate,
            observationRegistry,
        )
    }
}
