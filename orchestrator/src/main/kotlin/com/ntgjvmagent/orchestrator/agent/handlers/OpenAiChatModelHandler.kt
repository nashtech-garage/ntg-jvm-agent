package com.ntgjvmagent.orchestrator.agent.handlers

import com.ntgjvmagent.orchestrator.component.SimpleApiKey
import com.ntgjvmagent.orchestrator.enum.ProviderType
import com.ntgjvmagent.orchestrator.model.ChatModelConfig
import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.model.tool.ToolCallingManager
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
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
    @Qualifier("noRetryTemplate")
    private val retryTemplate: RetryTemplate,
    private val observationRegistry: ObservationRegistry,
) : ChatModelHandler {
    override fun supports(providerType: ProviderType) = providerType == ProviderType.OPENAI

    override fun createChatModel(config: ChatModelConfig): ChatModel {
        val openAiApi =
            OpenAiApi(
                config.baseUrl,
                SimpleApiKey(config.apiKey),
                LinkedMultiValueMap(),
                config.chatCompletionsPath,
                config.embeddingsPath,
                RestClient.builder(),
                WebClient.builder(),
                DefaultResponseErrorHandler(),
            )

        val options =
            OpenAiChatOptions
                .builder()
                .model(config.modelName)
                .build()

        return OpenAiChatModel(openAiApi, options, toolCallingManager, retryTemplate, observationRegistry)
    }
}
