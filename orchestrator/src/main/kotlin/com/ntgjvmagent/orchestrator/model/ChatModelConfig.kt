package com.ntgjvmagent.orchestrator.model

import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.model.ProviderType
import java.math.BigDecimal

/**
 * Configuration for creating ChatModel instances.
 * Holds all parameters needed to create a chat model for a specific provider.
 */
data class ChatModelConfig(
    val providerType: ProviderType,
    // Provider infra (agent-scoped)
    val baseUrl: String,
    val apiKey: String,
    val chatCompletionsPath: String,
    val embeddingsPath: String = "/embeddings",
    // Agent behavior
    val modelName: String,
    val temperature: BigDecimal? = null,
    val topP: BigDecimal? = null,
    val maxTokens: Int? = null,
    val frequencyPenalty: BigDecimal? = null,
    val presencePenalty: BigDecimal? = null,
) {
    companion object {
        fun fromAgent(agent: Agent): ChatModelConfig =
            ChatModelConfig(
                providerType = agent.provider,
                baseUrl = agent.baseUrl,
                apiKey = agent.apiKey,
                chatCompletionsPath = agent.chatCompletionsPath,
                modelName = agent.model,
                temperature = agent.temperature,
                topP = agent.topP,
                maxTokens = agent.maxTokens,
                frequencyPenalty = agent.frequencyPenalty,
                presencePenalty = agent.presencePenalty,
            )
    }
}
