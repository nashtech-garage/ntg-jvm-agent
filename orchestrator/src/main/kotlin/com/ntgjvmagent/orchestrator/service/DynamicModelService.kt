package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.component.SimpleApiKey
import com.ntgjvmagent.orchestrator.embedding.ReactiveEmbeddingModel
import com.ntgjvmagent.orchestrator.embedding.SpringAiEmbeddingModelAdapter
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.document.MetadataMode
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
import org.springframework.ai.embedding.EmbeddingModel as SpringEmbeddingModel

@Service
class DynamicModelService(
    private val toolCallingManager: ToolCallingManager,
    private val noRetryTemplate: RetryTemplate,
    private val observationRegistry: ObservationRegistry,
    private val agentRepo: AgentRepository,
) {
    /**
     * Cache stores:
     *  1) ChatModel
     *  2) Wrapped reactive EmbeddingModel
     *  3) Raw Spring AI EmbeddingModel (needed for PgVectorStore)
     */
    private val cache = ConcurrentHashMap<UUID, Triple<ChatModel, ReactiveEmbeddingModel, SpringEmbeddingModel>>()

    fun getChatModel(agentId: UUID): ChatModel = cache.computeIfAbsent(agentId) { createModels(it) }.first

    /** Reactive embedding model (adapter-wrapped) used by EmbeddingService */
    fun getEmbeddingModel(agentId: UUID): ReactiveEmbeddingModel =
        cache.computeIfAbsent(agentId) { createModels(it) }.second

    /** Raw Spring AI embedding model used by PgVectorStore */
    fun getRawSpringEmbeddingModel(agentId: UUID): SpringEmbeddingModel =
        cache.computeIfAbsent(agentId) { createModels(it) }.third

    /**
     * Atomic reload of caching entry for a given agent.
     * Prevents concurrency races and duplicate rebuilds.
     */
    fun reloadModelsForAgent(agentId: UUID) {
        cache.compute(agentId) { _, _ ->
            createModels(agentId)
        }
    }

    /**
     * Prefer reload() instead of removing.
     * Never leave the cache empty in concurrent systems.
     */
    fun invalidateCacheForAgent(agentId: UUID) {
        reloadModelsForAgent(agentId)
    }

    // ---------------------------------------------------------------------
    // Build Chat + Embedding models (Spring + wrapped)
    // ---------------------------------------------------------------------
    private fun createModels(
        agentId: UUID,
    ): Triple<
        ChatModel,
        ReactiveEmbeddingModel,
        SpringEmbeddingModel,
    > {
        val agentConfig = agentRepo.findById(agentId).orElseThrow()

        val api =
            createOpenAiApi(
                baseUrl = agentConfig.baseUrl,
                apiKey = agentConfig.apiKey,
                chatCompletionsPath = agentConfig.chatCompletionsPath,
                embeddingsPath = agentConfig.embeddingsPath,
            )

        val chatModel = createChatModel(api, agentConfig.model)
        val springEmbeddingModel = createSpringEmbeddingModel(api, agentConfig.embeddingModel, agentConfig.dimension)
        val wrappedEmbeddingModel = SpringAiEmbeddingModelAdapter(springEmbeddingModel)

        return Triple(chatModel, wrappedEmbeddingModel, springEmbeddingModel)
    }

    // ---------------------------------------------------------------------
    // Chat Model
    // ---------------------------------------------------------------------
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

    // ---------------------------------------------------------------------
    // RAW Spring AI EmbeddingModel
    // This is required by PgVectorStore (synchronous)
    // ---------------------------------------------------------------------
    private fun createSpringEmbeddingModel(
        api: OpenAiApi,
        modelName: String,
        dimension: Int,
    ): SpringEmbeddingModel {
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

    // ---------------------------------------------------------------------
    // OpenAI API Client
    // ---------------------------------------------------------------------
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
