package com.ntgjvmagent.orchestrator.service

import com.azure.ai.openai.OpenAIClientBuilder
import com.azure.core.credential.AzureKeyCredential
import com.ntgjvmagent.orchestrator.component.SimpleApiKey
import com.ntgjvmagent.orchestrator.dto.response.AgentResponseDto
import com.ntgjvmagent.orchestrator.embedding.runtime.ReactiveEmbeddingModel
import com.ntgjvmagent.orchestrator.embedding.runtime.adapter.SpringAiEmbeddingModelAdapter
import com.ntgjvmagent.orchestrator.mapper.AgentMapper
import com.ntgjvmagent.orchestrator.enum.ProviderType
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import com.ntgjvmagent.orchestrator.utils.Quadruple
import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.azure.openai.AzureOpenAiEmbeddingModel
import org.springframework.ai.azure.openai.AzureOpenAiEmbeddingOptions
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.document.MetadataMode
import org.springframework.ai.model.tool.ToolCallingManager
import org.springframework.ai.openai.OpenAiEmbeddingModel
import org.springframework.ai.openai.OpenAiEmbeddingOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.core.retry.RetryTemplate
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestClient
import org.springframework.web.reactive.function.client.WebClient
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.springframework.ai.embedding.EmbeddingModel as SpringEmbeddingModel

@Service
class DynamicModelService(
    private val chatModelProvider: ChatModelProvider,
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

        val api =
            createOpenAiApi(
                baseUrl = agent.baseUrl,
                apiKey = agent.apiKey,
                chatCompletionsPath = agent.chatCompletionsPath,
                embeddingsPath = agent.embeddingsPath,
            )
        // Use ChatModelProvider to create chat model
        val chatModel = chatModelProvider.createChatModel(
            providerType = agentConfig.provider,
            baseUrl = agentConfig.baseUrl,
            apiKey = agentConfig.apiKey,
            modelName = agentConfig.model,
            temperature = agentConfig.temperature,
            topP = agentConfig.topP,
            maxTokens = agentConfig.maxTokens,
            frequencyPenalty = agentConfig.frequencyPenalty,
            presencePenalty = agentConfig.presencePenalty,
            chatCompletionsPath = agentConfig.chatCompletionsPath,
            embeddingsPath = agentConfig.embeddingsPath,
        )

        val chatModel = createChatModel(api, agent.model)
        val springEmbeddingModel = createSpringEmbeddingModel(api, agent.embeddingModel, agent.dimension)
        // Create embedding model based on provider type
        val springEmbeddingModel = createEmbeddingModel(agentConfig)

        val wrappedEmbeddingModel = SpringAiEmbeddingModelAdapter(springEmbeddingModel)
        val agentConfig = AgentMapper.toResponse(agent)

        return Quadruple(chatModel, wrappedEmbeddingModel, springEmbeddingModel, agentConfig)
        return Triple(chatModel, wrappedEmbeddingModel, springEmbeddingModel)
    }

    // =====================================================================
    // Embedding Models (provider-based)
    // =====================================================================
    private fun createEmbeddingModel(agentConfig: com.ntgjvmagent.orchestrator.entity.agent.Agent): SpringEmbeddingModel {
        return when (agentConfig.provider) {
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
                throw UnsupportedOperationException("Ollama embedding is not yet supported. Support will be added in future versions.")

            ProviderType.BEDROCK ->
                throw UnsupportedOperationException("Bedrock embedding is not yet supported. Support will be added in future versions.")

            ProviderType.ANTHROPIC ->
                throw UnsupportedOperationException("Anthropic embedding is not yet supported. Support will be added in future versions.")
        }
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
        embeddingModel: String, // Azure: deployment name embedding
        dimension: Int,
    ): SpringEmbeddingModel {
        // 1) Azure OpenAI client (Azure SDK)
        val client =
            OpenAIClientBuilder()
                .credential(AzureKeyCredential(apiKey))
                .endpoint(baseUrl)
                .buildClient()

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
