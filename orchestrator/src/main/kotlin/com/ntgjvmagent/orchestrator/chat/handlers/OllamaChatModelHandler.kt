package com.ntgjvmagent.orchestrator.chat.handlers

import com.ntgjvmagent.orchestrator.model.ChatModelConfig
import com.ntgjvmagent.orchestrator.model.ProviderType
import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.model.tool.ToolCallingManager
import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.ai.ollama.api.OllamaApi
import org.springframework.ai.ollama.api.OllamaChatOptions
import org.springframework.core.retry.RetryTemplate
import org.springframework.stereotype.Service
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestClient
import org.springframework.web.reactive.function.client.WebClient

/**
 * Ollama ChatModel Handler
 * Creates Ollama ChatModel with OllamaOptions.
 * baseUrl: http://localhost:11434 (default Ollama server)
 * modelName: llama3.1, mistral, qwen2.5, etc.
 */
@Service
class OllamaChatModelHandler(
    private val toolCallingManager: ToolCallingManager,
    private val noRetryTemplate: RetryTemplate,
    private val observationRegistry: ObservationRegistry,
) : ChatModelHandler {
    override fun supports(providerType: ProviderType) = providerType == ProviderType.OLLAMA

    override fun createChatModel(config: ChatModelConfig): ChatModel {
        val ollamaApi =
            OllamaApi
                .builder()
                .baseUrl(config.baseUrl) // e.g., "http://localhost:11434"
                .restClientBuilder(RestClient.builder())
                .webClientBuilder(WebClient.builder())
                .responseErrorHandler(DefaultResponseErrorHandler())
                .build()

        val optionsBuilder =
            OllamaChatOptions
                .builder()
                .model(config.modelName)

        // Apply optional parameters
        config.temperature?.let { optionsBuilder.temperature(it.toDouble()) }
        config.topP?.let { optionsBuilder.topP(it.toDouble()) }
        // Ollama uses numPredict instead of maxTokens
        config.maxTokens?.let { optionsBuilder.numPredict(it) }

        return OllamaChatModel
            .builder()
            .ollamaApi(ollamaApi)
            .defaultOptions(optionsBuilder.build())
            .toolCallingManager(toolCallingManager)
            .retryTemplate(noRetryTemplate)
            .observationRegistry(observationRegistry)
            .build()
    }
}
