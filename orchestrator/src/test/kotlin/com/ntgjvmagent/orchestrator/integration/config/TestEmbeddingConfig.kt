package com.ntgjvmagent.orchestrator.integration.config

import com.ntgjvmagent.orchestrator.embedding.EmbeddingService
import com.ntgjvmagent.orchestrator.embedding.ReactiveEmbeddingModel
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import com.ntgjvmagent.orchestrator.service.DynamicModelService
import io.micrometer.observation.ObservationRegistry
import io.mockk.mockk
import org.springframework.ai.document.Document
import org.springframework.ai.embedding.BatchingStrategy
import org.springframework.ai.embedding.Embedding
import org.springframework.ai.embedding.EmbeddingOptions
import org.springframework.ai.embedding.EmbeddingRequest
import org.springframework.ai.embedding.EmbeddingResponse
import org.springframework.ai.model.tool.ToolCallingManager
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.retry.support.RetryTemplate
import reactor.core.publisher.Mono
import java.util.UUID
import kotlin.collections.map
import org.springframework.ai.embedding.EmbeddingModel as SpringEmbeddingModel

/**
 * Test configuration providing a fake embedding model for tests.
 * Uses the same embedding dimension as production.
 */
@TestConfiguration
class TestEmbeddingConfig {
    // ---------------------------------------------------------------------
    // 1) ToolCallingManager Mock
    // ---------------------------------------------------------------------
    @Bean
    @Primary
    fun toolCallingManager(): ToolCallingManager = mockk(relaxed = true)

    // ---------------------------------------------------------------------
    // 2) Fake ReactiveEmbeddingModel (for EmbeddingService)
    // ---------------------------------------------------------------------
    @Bean
    @Primary
    fun fakeReactiveEmbeddingModel(): ReactiveEmbeddingModel =
        object : ReactiveEmbeddingModel {
            private val dims = 1536

            override fun embedReactive(text: String): Mono<FloatArray> = Mono.just(FloatArray(dims) { 0.1f })

            override fun embedBatchReactive(texts: List<String>): Mono<List<FloatArray>> {
                val safeTexts = if (texts.isEmpty()) listOf("dummy") else texts
                return Mono.just(
                    safeTexts.map { FloatArray(dims) { 0.1f } },
                )
            }
        }

    // ---------------------------------------------------------------------
    // 3) Fake SpringEmbeddingModel (used by PgVectorStore)
    // ---------------------------------------------------------------------
    @Bean
    @Primary
    fun fakeSpringEmbeddingModel(): SpringEmbeddingModel =
        object : SpringEmbeddingModel {
            private val dims = 1536

            override fun embed(text: String): FloatArray = FloatArray(dims) { 0.1f }

            override fun embed(texts: List<String>): List<FloatArray> {
                val safeTexts = texts.ifEmpty { listOf("dummy") }
                return safeTexts.map { FloatArray(dims) { 0.1f } }
            }

            override fun embed(document: Document): FloatArray = FloatArray(dims) { 0.1f }

            override fun embed(
                documents: MutableList<Document>,
                options: EmbeddingOptions,
                batchingStrategy: BatchingStrategy,
            ): MutableList<FloatArray> = documents.map { FloatArray(dims) { 0.0f } }.toMutableList()

            override fun call(request: EmbeddingRequest): EmbeddingResponse =
                EmbeddingResponse(
                    request.instructions.mapIndexed { idx, _ ->
                        Embedding(FloatArray(dims) { 0.1f }, idx)
                    },
                )
        }

    // ---------------------------------------------------------------------
    // 4) DynamicModelService override
    // ---------------------------------------------------------------------
    @Bean
    @Primary
    fun testDynamicModelService(
        fakeReactiveEmbeddingModel: ReactiveEmbeddingModel,
        fakeSpringEmbeddingModel: SpringEmbeddingModel,
        toolCallingManager: ToolCallingManager,
        agentRepo: AgentRepository, // REAL repository
    ): DynamicModelService =
        object : DynamicModelService(
            toolCallingManager = toolCallingManager,
            noRetryTemplate = RetryTemplate(),
            observationRegistry = ObservationRegistry.create(),
            agentRepo = agentRepo,
        ) {
            override fun getEmbeddingModel(agentId: UUID): ReactiveEmbeddingModel = fakeReactiveEmbeddingModel

            override fun getRawSpringEmbeddingModel(agentId: UUID): SpringEmbeddingModel = fakeSpringEmbeddingModel
        }

    // ---------------------------------------------------------------------
    // 5) Fake EmbeddingService (simple synchronous override)
    // ---------------------------------------------------------------------
    @Bean
    @Primary
    fun fakeEmbeddingService(): EmbeddingService =
        object : EmbeddingService(mockk(), mockk(), mockk(), mockk()) {
            private val dims = 1536

            override fun embed(
                agentId: UUID,
                text: String,
                correlationId: String,
            ): FloatArray = FloatArray(dims) { 0.1f }

            override fun embedBatch(
                agentId: UUID,
                texts: List<String>,
                correlationId: String,
            ): List<FloatArray> {
                val safeTexts = texts.ifEmpty { listOf("dummy") }
                return safeTexts.map { FloatArray(dims) { 0.1f } }
            }
        }
}
