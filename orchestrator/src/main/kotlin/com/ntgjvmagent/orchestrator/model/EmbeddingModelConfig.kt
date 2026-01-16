package com.ntgjvmagent.orchestrator.model

import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.enum.ProviderType

data class EmbeddingModelConfig(
    val providerType: ProviderType,
    val baseUrl: String,
    val apiKey: String,
    val chatCompletionsPath: String,
    val embeddingsPath: String,
    val embeddingModel: String,
    val dimension: Int,
) {
    companion object {
        fun fromAgent(agent: Agent) =
            EmbeddingModelConfig(
                providerType = agent.provider,
                baseUrl = agent.baseUrl,
                apiKey = agent.apiKey,
                chatCompletionsPath = agent.chatCompletionsPath,
                embeddingsPath = agent.embeddingsPath,
                embeddingModel = agent.embeddingModel,
                dimension = agent.dimension,
            )
    }
}
