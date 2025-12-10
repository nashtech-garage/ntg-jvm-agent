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
import java.util.concurrent.ConcurrentHashMap

@Service
class DynamicModelService(
    private val toolCallingManager: ToolCallingManager,
    private val noRetryTemplate: RetryTemplate,
    private val observationRegistry: ObservationRegistry,
    private val agentRepo: AgentRepository,
) {
    /** THREAD-SAFE CACHE **/
    private val cache = ConcurrentHashMap<UUID, Pair<ChatModel, EmbeddingModel>>()

    fun getChatModel(agentId: UUID): ChatModel = cache.computeIfAbsent(agentId) { createModels(agentId) }.first

    fun getEmbeddingModel(agentId: UUID): EmbeddingModel =
        cache.computeIfAbsent(agentId) { createModels(agentId) }.second

    fun invalidateCacheForAgent(agentId: UUID) {
        cache.remove(agentId)
    }

    /** Build both chat + embedding models **/
    private fun createModels(agentId: UUID): Pair<ChatModel, EmbeddingModel> {
        val config = agentRepo.findById(agentId).orElseThrow()

        val api =
            createOpenAiApi(
                baseUrl = config.baseUrl,
                apiKey = config.apiKey,
                chatCompletionsPath = config.chatCompletionsPath,
                embeddingsPath = config.embeddingsPath,
            )

        val chat = createChatModel(api, config.model)
        val embed = createEmbeddingModel(api, config.embeddingModel, config.dimension)

        return chat to embed
    }

    /** Chat model - NO SPRING RETRY **/
    private fun createChatModel(
        api: OpenAiApi,
        modelName: String,
    ): ChatModel {
        val options =
            OpenAiChatOptions
                .builder()
                .model(modelName)
                .build()

        return OpenAiChatModel(
            api,
            options,
            toolCallingManager,
            noRetryTemplate,
            observationRegistry,
        )
    }

    /** Embedding model - NO SPRING RETRY **/
    private fun createEmbeddingModel(
        api: OpenAiApi,
        modelName: String,
        dimension: Int,
    ): EmbeddingModel {
        val options =
            OpenAiEmbeddingOptions
                .builder()
                .model(modelName)
                .dimensions(dimension)
                .build()

        return OpenAiEmbeddingModel(
            api,
            MetadataMode.EMBED,
            options,
            noRetryTemplate,
            observationRegistry,
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
