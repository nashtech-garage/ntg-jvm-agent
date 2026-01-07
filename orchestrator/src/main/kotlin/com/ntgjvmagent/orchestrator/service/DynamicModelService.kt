package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.agent.ModelOrchestrator
import com.ntgjvmagent.orchestrator.dto.response.AgentResponseDto
import com.ntgjvmagent.orchestrator.embedding.runtime.ReactiveEmbeddingModel
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import com.ntgjvmagent.orchestrator.utils.Quadruple
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.document.MetadataMode
import org.springframework.ai.openai.OpenAiEmbeddingModel
import org.springframework.ai.openai.OpenAiEmbeddingOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.core.retry.RetryTemplate
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestClient
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.springframework.ai.embedding.EmbeddingModel as SpringEmbeddingModel

@Service
@Suppress("TooManyFunctions")
class DynamicModelService(
    private val modelOrchestrator: ModelOrchestrator,
    private val agentRepo: AgentRepository,
) {
    /**
     * Cache stores:
     *  1) ChatModel
     *  2) Wrapped reactive EmbeddingModel
     *  3) Raw Spring AI EmbeddingModel (needed for PgVectorStore)
     */
    private val cache =
        ConcurrentHashMap<UUID, Quadruple<ChatModel, ReactiveEmbeddingModel, SpringEmbeddingModel, AgentResponseDto>>()

    fun getChatModel(agentId: UUID): ChatModel = cache.computeIfAbsent(agentId) { createModels(it) }.first

    /** Reactive embedding model (adapter-wrapped) used by EmbeddingService */
    fun getEmbeddingModel(agentId: UUID): ReactiveEmbeddingModel =
        cache.computeIfAbsent(agentId) { createModels(it) }.second

    /** Raw Spring AI embedding model used by PgVectorStore */
    fun getRawSpringEmbeddingModel(agentId: UUID): SpringEmbeddingModel =
        cache.computeIfAbsent(agentId) { createModels(it) }.third

    fun getAgentConfig(agentId: UUID): AgentResponseDto = cache.computeIfAbsent(agentId) { createModels(it) }.fourth

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

    private fun createModels(
        agentId: UUID,
    ): Quadruple<ChatModel, ReactiveEmbeddingModel, SpringEmbeddingModel, AgentResponseDto> {
        val agent = agentRepo.findById(agentId).orElseThrow()
        return modelOrchestrator.create(agent)
    }
}
