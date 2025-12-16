package com.ntgjvmagent.orchestrator.service

import com.azure.ai.openai.OpenAIClientBuilder
import com.azure.core.credential.AzureKeyCredential
import com.ntgjvmagent.orchestrator.component.SimpleApiKey
import com.ntgjvmagent.orchestrator.enum.ProviderType
import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.azure.openai.AzureOpenAiChatModel
import org.springframework.ai.azure.openai.AzureOpenAiChatOptions
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.model.tool.ToolCallingManager
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestClient
import org.springframework.web.reactive.function.client.WebClient
import java.math.BigDecimal

/**
 * Service to create ChatModel instances based on provider type and configuration.
 *
 * Supports 5 LLM providers with native Spring AI implementations:
 * - OpenAI: OpenAiChatModel + OpenAiChatOptions
 * - Azure OpenAI: AzureOpenAiChatModel + AzureOpenAiChatOptions
 * - Ollama: OllamaChatModel + OllamaChatOptions
 * - Amazon Bedrock: Native BedrockChatModel + BedrockChatOptions
 * - Anthropic: Native AnthropicChatModel + AnthropicChatOptions
 */
@Service
class ChatModelProvider(
    private val toolCallingManager: ToolCallingManager,
    private val retryTemplate: RetryTemplate,
    private val observationRegistry: ObservationRegistry,
) {
    fun createChatModel(
        providerType: ProviderType,
        baseUrl: String,
        apiKey: String,
        modelName: String,
        temperature: BigDecimal? = null,
        topP: BigDecimal? = null,
        maxTokens: Int? = null,
        frequencyPenalty: BigDecimal? = null,
        presencePenalty: BigDecimal? = null,
        chatCompletionsPath: String = "/v1/chat/completions",
        embeddingsPath: String = "/v1/embeddings",
        additionalParams: Map<String, Any>? = null,
    ): ChatModel =
        when (providerType) {
            ProviderType.OPENAI ->
                createOpenAiChatModel(
                    baseUrl,
                    apiKey,
                    modelName,
                    temperature,
                    topP,
                    maxTokens,
                    frequencyPenalty,
                    presencePenalty,
                    chatCompletionsPath,
                    embeddingsPath,
                )

            ProviderType.AZURE_OPENAI ->
                createAzureOpenAiChatModel(
                    baseUrl,
                    apiKey,
                    modelName,
                    temperature,
                    topP,
                    maxTokens,
                    frequencyPenalty,
                    presencePenalty,
                )

            ProviderType.OLLAMA ->
                throw UnsupportedOperationException(
                    "Ollama is not yet supported. Support will be added in future versions.",
                )

            ProviderType.BEDROCK ->
                throw UnsupportedOperationException(
                    "Bedrock is not yet supported. Support will be added in future versions.",
                )

            ProviderType.ANTHROPIC ->
                throw UnsupportedOperationException(
                    "Anthropic is not yet supported. Support will be added in future versions.",
                )
        }

    /**
     * Creates OpenAI ChatModel with OpenAiChatOptions.
     * baseUrl: https://api.openai.com/v1
     * model: gpt-4o, gpt-4-turbo, gpt-3.5-turbo
     */
    private fun createOpenAiChatModel(
        baseUrl: String,
        apiKey: String,
        modelName: String,
        temperature: BigDecimal?,
        topP: BigDecimal?,
        maxTokens: Int?,
        frequencyPenalty: BigDecimal?,
        presencePenalty: BigDecimal?,
        chatCompletionsPath: String,
        embeddingsPath: String,
    ): ChatModel {
        val openAiApi =
            OpenAiApi(
                baseUrl,
                SimpleApiKey(apiKey),
                LinkedMultiValueMap(),
                chatCompletionsPath,
                embeddingsPath,
                RestClient.builder(),
                WebClient.builder(),
                DefaultResponseErrorHandler(),
            )

        val options =
            OpenAiChatOptions
                .builder()
                .model(modelName)
                .build()

        return OpenAiChatModel(openAiApi, options, toolCallingManager, retryTemplate, observationRegistry)
    }

    /**
     * Creates Azure OpenAI ChatModel using native AzureOpenAiChatModel.
     * baseUrl: https://resource.openai.azure.com/
     * modelName: deployment name (e.g., gpt-4o)
     */
    private fun createAzureOpenAiChatModel(
        baseUrl: String, // https://<resource>.openai.azure.com
        apiKey: String,
        modelName: String, // Azure: deployment name
        temperature: BigDecimal?,
        topP: BigDecimal?,
        maxTokens: Int?,
        frequencyPenalty: BigDecimal?,
        presencePenalty: BigDecimal?,
    ): ChatModel {
        val clientBuilder =
            OpenAIClientBuilder()
                .credential(AzureKeyCredential(apiKey))
                .endpoint(baseUrl)

        val optionsBuilder =
            AzureOpenAiChatOptions
                .builder()
                .deploymentName(modelName) // Azure use deploymentName
                .temperature(temperature?.toDouble() ?: 0.7)
                .topP(topP?.toDouble() ?: 1.0)
                .frequencyPenalty(frequencyPenalty?.toDouble() ?: 0.0)
                .presencePenalty(presencePenalty?.toDouble() ?: 0.0)
                .maxTokens(maxTokens)

        val options = optionsBuilder.build()

        return AzureOpenAiChatModel
            .builder()
            .openAIClientBuilder(clientBuilder)
            .defaultOptions(options)
            .toolCallingManager(toolCallingManager)
            .observationRegistry(observationRegistry)
            .build()
    }

    /**
     * Creates Ollama ChatModel using native OllamaChatModel.
     * TODO: Implement Ollama support in future versions.
     * baseUrl: http://localhost:11434
     * model: llama2, mistral, neural-chat
     */
    private fun createOllamaChatModel(
        baseUrl: String,
        modelName: String,
        temperature: BigDecimal?,
        topP: BigDecimal?,
        maxTokens: Int?,
    ): ChatModel = throw UnsupportedOperationException("Ollama support coming soon")

    /**
     * Creates Amazon Bedrock ChatModel using native BedrockChatModel.
     * TODO: Implement Bedrock support in future versions.
     * model: anthropic.claude-sonnet-4-5-20250929-v1:0
     * Requires AWS credentials configured
     */
    private fun createBedrockChatModel(
        modelName: String,
        temperature: BigDecimal?,
        topP: BigDecimal?,
        maxTokens: Int?,
        additionalParams: Map<String, Any>?,
    ): ChatModel = throw UnsupportedOperationException("Bedrock support coming soon")

    /**
     * Creates Anthropic ChatModel using native AnthropicChatModel.
     * TODO: Implement Anthropic support in future versions.
     * model: claude-3-5-sonnet-20241022, claude-3-opus-20240229
     * baseUrl: https://api.anthropic.com (default)
     */
    private fun createAnthropicChatModel(
        apiKey: String,
        modelName: String,
        temperature: BigDecimal?,
        topP: BigDecimal?,
        maxTokens: Int?,
    ): ChatModel = throw UnsupportedOperationException("Anthropic support coming soon")
}
