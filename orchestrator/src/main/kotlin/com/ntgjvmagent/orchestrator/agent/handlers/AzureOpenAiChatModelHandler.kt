package com.ntgjvmagent.orchestrator.agent.handlers

import com.azure.ai.openai.OpenAIClientBuilder
import com.azure.core.credential.AzureKeyCredential
import com.azure.core.http.policy.FixedDelayOptions
import com.azure.core.http.policy.RetryOptions
import com.ntgjvmagent.orchestrator.enum.ProviderType
import com.ntgjvmagent.orchestrator.model.ChatModelConfig
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
    override fun supports(providerType: ProviderType) = providerType == ProviderType.AZURE_OPENAI

    override fun createChatModel(config: ChatModelConfig): ChatModel {
        val clientBuilder =
            OpenAIClientBuilder()
                .credential(AzureKeyCredential(config.apiKey))
                .retryOptions(RetryOptions(FixedDelayOptions(0, Duration.ZERO)))
                .endpoint(config.baseUrl)

        val optionsBuilder =
            AzureOpenAiChatOptions
                .builder()
                .deploymentName(config.modelName) // Azure use deploymentName
                .temperature(config.temperature?.toDouble() ?: 0.7)
                .topP(config.topP?.toDouble() ?: 1.0)
                .frequencyPenalty(config.frequencyPenalty?.toDouble() ?: 0.0)
                .presencePenalty(config.presencePenalty?.toDouble() ?: 0.0)

        if (config.maxTokens != null) {
            optionsBuilder.maxTokens(config.maxTokens)
        }

        val options = optionsBuilder.build()

        return AzureOpenAiChatModel
            .builder()
            .openAIClientBuilder(clientBuilder)
            .defaultOptions(options)
            .toolCallingManager(toolCallingManager)
            .observationRegistry(observationRegistry)
            .build()
    }
}

