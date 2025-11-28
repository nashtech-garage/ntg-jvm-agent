package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.component.SimpleApiKey
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.document.MetadataMode
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.model.tool.ToolCallingManager
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.OpenAiEmbeddingModel
import org.springframework.ai.openai.OpenAiEmbeddingOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestClient
import org.springframework.web.reactive.function.client.WebClient
import java.util.UUID

@Service
class DynamicModelService(
    private val toolCallingManager: ToolCallingManager,
    private val retryTemplate: RetryTemplate,
    private val observationRegistry: ObservationRegistry,
    private val agentRepo: AgentRepository,
) {
    private val cache = mutableMapOf<UUID, Pair<ChatModel, EmbeddingModel>>()

    fun getChatModel(agentId: UUID): ChatModel = cache.getOrPut(agentId) { createModels(agentId) }.first

    fun getEmbeddingModel(agentId: UUID): EmbeddingModel = cache.getOrPut(agentId) { createModels(agentId) }.second

    fun invalidateCacheForAgent(agentId: UUID) {
        cache[agentId] = createModels(agentId)
    }

    private fun createModels(agentId: UUID): Pair<ChatModel, EmbeddingModel> {
        val config = agentRepo.findById(agentId).orElseThrow()
        val chatModel =
            createChatModel(
                config.baseUrl,
                config.apiKey,
                config.model,
                config.chatCompletionsPath,
                config.embeddingsPath,
            )
        val embeddingModel =
            createEmbeddingModel(
                config.baseUrl,
                config.apiKey,
                config.chatCompletionsPath,
                config.embeddingsPath,
                config.embeddingModel,
                config.dimension,
            )

        return chatModel to embeddingModel
    }

    private fun createChatModel(
        baseUrl: String,
        apiKey: String,
        modelName: String,
        chatCompletionsPath: String,
        embeddingsPath: String,
    ): ChatModel {
        val openAiApi = createOpenAiApi(baseUrl, apiKey, chatCompletionsPath, embeddingsPath)

        val options =
            OpenAiChatOptions
                .builder()
                .model(modelName)
                .build()

        return OpenAiChatModel(
            openAiApi,
            options,
            toolCallingManager,
            retryTemplate,
            observationRegistry,
        )
    }

    private fun createEmbeddingModel(
        baseUrl: String,
        apiKey: String,
        chatCompletionsPath: String,
        embeddingsPath: String,
        embeddingModel: String,
        dimension: Int,
    ): EmbeddingModel {
        val api = createOpenAiApi(baseUrl, apiKey, chatCompletionsPath, embeddingsPath)

        return OpenAiEmbeddingModel(
            api,
            MetadataMode.EMBED,
            OpenAiEmbeddingOptions
                .builder()
                .model(embeddingModel)
                .dimensions(dimension)
                .build(),
        )
    }

    private fun createOpenAiApi(
        baseUrl: String,
        apiKey: String,
        chatCompletionsPath: String,
        embeddingsPath: String,
    ): OpenAiApi =
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
}
