package com.ntgjvmagent.orchestrator.model

import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.enum.ProviderType
import java.math.BigDecimal

/**
 * Configuration for creating ChatModel instances.
 * Holds all parameters needed to create a chat model for a specific provider.
 */
data class ChatModelConfig(
    val providerType: ProviderType,
    val baseUrl: String,
    val apiKey: String,
    val modelName: String,
    val temperature: BigDecimal? = null,
    val topP: BigDecimal? = null,
    val maxTokens: Int? = null,
    val frequencyPenalty: BigDecimal? = null,
    val presencePenalty: BigDecimal? = null,
    val chatCompletionsPath: String = "/v1/chat/completions",
    val embeddingsPath: String = "/v1/embeddings",
) {
    companion object {
        fun fromAgent(agent: Agent): ChatModelConfig =
            ChatModelConfig(
                providerType = agent.provider,
                baseUrl = agent.baseUrl,
                apiKey = agent.apiKey,
                modelName = agent.model,
                temperature = agent.temperature,
                topP = agent.topP,
                maxTokens = agent.maxTokens,
                frequencyPenalty = agent.frequencyPenalty,
                presencePenalty = agent.presencePenalty,
                chatCompletionsPath = agent.chatCompletionsPath ?: "/v1/chat/completions",
                embeddingsPath = agent.embeddingsPath ?: "/v1/embeddings",
            )
    }
}
