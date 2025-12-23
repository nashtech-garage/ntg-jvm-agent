package com.ntgjvmagent.orchestrator.service

import com.azure.ai.openai.OpenAIClientBuilder
import com.azure.core.credential.AzureKeyCredential
import com.azure.core.http.policy.FixedDelayOptions
import com.azure.core.http.policy.RetryOptions
import com.ntgjvmagent.orchestrator.component.SimpleApiKey
import com.ntgjvmagent.orchestrator.dto.response.AgentResponseDto
import com.ntgjvmagent.orchestrator.embedding.runtime.ReactiveEmbeddingModel
import com.ntgjvmagent.orchestrator.embedding.runtime.adapter.SpringAiEmbeddingModelAdapter
import com.ntgjvmagent.orchestrator.mapper.AgentMapper
import com.ntgjvmagent.orchestrator.enum.ProviderType
import com.ntgjvmagent.orchestrator.provider.ChatModelProvider
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import com.ntgjvmagent.orchestrator.utils.Quadruple
import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.azure.openai.AzureOpenAiEmbeddingModel
import org.springframework.ai.azure.openai.AzureOpenAiEmbeddingOptions
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
    private val chatModelProvider: ChatModelProvider,
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

    // ---------------------------------------------------------------------
    // Build Chat + Embedding models (Spring + wrapped)
    // ---------------------------------------------------------------------
    private fun createModels(
        agentId: UUID,
    ): Quadruple<
        ChatModel,
        ReactiveEmbeddingModel,
        SpringEmbeddingModel,
        AgentResponseDto,
    > {
        val agent = agentRepo.findById(agentId).orElseThrow()

        // Use ChatModelProvider to create chat model
        val chatModel =
            chatModelProvider.createChatModel(
                providerType = agent.provider,
                baseUrl = agent.baseUrl,
                apiKey = agent.apiKey,
                modelName = agent.model,
                temperature = agent.temperature,
                topP = agent.topP,
                maxTokens = agent.maxTokens,
                frequencyPenalty = agent.frequencyPenalty,
                presencePenalty = agent.presencePenalty,
                chatCompletionsPath = agent.chatCompletionsPath,
                embeddingsPath = agent.embeddingsPath,
            )

        // Create embedding model based on provider type (not using OpenAiApi)
        val springEmbeddingModel = createEmbeddingModel(agent)
        val wrappedEmbeddingModel = SpringAiEmbeddingModelAdapter(springEmbeddingModel)
        val agentConfig = AgentMapper.toResponse(agent)

        return Quadruple(chatModel, wrappedEmbeddingModel, springEmbeddingModel, agentConfig)
    }

    // =====================================================================
    // Embedding Models (provider-based)
    // =====================================================================
    private fun createEmbeddingModel(
        agentConfig: com.ntgjvmagent.orchestrator.entity.agent.Agent,
    ): SpringEmbeddingModel =
        when (agentConfig.provider) {
            ProviderType.OPENAI ->
                createOpenAiEmbeddingModel(
                    baseUrl = agentConfig.baseUrl,
                    apiKey = agentConfig.apiKey,
                    chatCompletionsPath = agentConfig.chatCompletionsPath,
                    embeddingsPath = agentConfig.embeddingsPath,
                    embeddingModel = agentConfig.embeddingModel,
                    dimension = agentConfig.dimension,
                )

            ProviderType.AZURE_OPENAI ->
                createAzureOpenAiEmbeddingModel(
                    baseUrl = agentConfig.baseUrl,
                    apiKey = agentConfig.apiKey,
                    embeddingModel = agentConfig.embeddingModel,
                    dimension = agentConfig.dimension,
                )

            ProviderType.OLLAMA ->
                throw UnsupportedOperationException(
                    "Ollama embedding is not yet supported. Support will be added in future versions.",
                )

            ProviderType.BEDROCK ->
                throw UnsupportedOperationException(
                    "Bedrock embedding is not yet supported. Support will be added in future versions.",
                )

            ProviderType.ANTHROPIC ->
                throw UnsupportedOperationException(
                    "Anthropic embedding is not yet supported. Support will be added in future versions.",
                )
        }

    // =====================================================================
    // OpenAI Embedding Model
    // =====================================================================
    private fun createOpenAiEmbeddingModel(
        baseUrl: String,
        apiKey: String,
        chatCompletionsPath: String,
        embeddingsPath: String,
        embeddingModel: String,
        dimension: Int,
    ): SpringEmbeddingModel {
        val api =
            createOpenAiApi(
                baseUrl = baseUrl,
                apiKey = apiKey,
                chatCompletionsPath = chatCompletionsPath,
                embeddingsPath = embeddingsPath,
            )

        return createSpringEmbeddingModel(api, embeddingModel, dimension)
    }

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

    // =====================================================================
    // Azure OpenAI Embedding Model
    // =====================================================================

    private fun createAzureOpenAiEmbeddingModel(
        baseUrl: String,
        apiKey: String,
        embeddingModel: String, // Azure: deployment name for embeddings
        dimension: Int,
    ): SpringEmbeddingModel {
        // Disable retries maxRetries = 0
        val retryOptions =
            RetryOptions(FixedDelayOptions(0, Duration.ZERO))
                // Never retry even if policy would consider it transient
                .setShouldRetryCondition { false }

        val client =
            OpenAIClientBuilder()
                .credential(AzureKeyCredential(apiKey))
                .endpoint(baseUrl)
                .retryOptions(retryOptions)
                .buildClient() // Use sync client for Spring AI

        // 2) Options for embedding
        val options =
            AzureOpenAiEmbeddingOptions
                .builder()
                .deploymentName(embeddingModel)
                .dimensions(dimension)
                .build()

        // 3) Embedding model (native Spring AI)
        return AzureOpenAiEmbeddingModel(
            client,
            MetadataMode.EMBED,
            options,
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
            HttpHeaders(),
            chatCompletionsPath,
            embeddingsPath,
            RestClient.builder(),
            WebClient.builder(),
            DefaultResponseErrorHandler(),
        )
}
