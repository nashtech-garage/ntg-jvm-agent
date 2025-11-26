package com.ntgjvmagent.orchestrator.integration.config

import com.ntgjvmagent.orchestrator.config.VectorEmbeddingProperties
import com.ntgjvmagent.orchestrator.service.DynamicModelService
import io.micrometer.observation.ObservationRegistry
import io.mockk.mockk
import org.springframework.ai.document.Document
import org.springframework.ai.embedding.BatchingStrategy
import org.springframework.ai.embedding.Embedding
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.embedding.EmbeddingOptions
import org.springframework.ai.embedding.EmbeddingRequest
import org.springframework.ai.embedding.EmbeddingResponse
import org.springframework.ai.model.tool.ToolCallingManager
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.retry.support.RetryTemplate
import java.util.UUID

/**
 * Test configuration providing a fake embedding model for tests.
 * Uses the same embedding dimension as production.
 */
@TestConfiguration
class TestEmbeddingConfig(
    private val properties: VectorEmbeddingProperties,
) {
    @Bean
    @Primary
    fun toolCallingManager(): ToolCallingManager = mockk(relaxed = true)

    @Bean
    @Primary
    fun fakeEmbeddingModel(): EmbeddingModel =
        object : EmbeddingModel {
            private val dims = properties.embeddingDimension

            override fun embed(text: String): FloatArray = FloatArray(dims) { 0.0f }

            override fun embed(texts: List<String>): List<FloatArray> = texts.map { FloatArray(dims) { 0.0f } }

            override fun embed(document: Document): FloatArray = FloatArray(dims) { 0.0f }

            override fun embed(
                documents: MutableList<Document>,
                options: EmbeddingOptions,
                batchingStrategy: BatchingStrategy,
            ): MutableList<FloatArray> = documents.map { FloatArray(dims) { 0.0f } }.toMutableList()

            override fun call(request: EmbeddingRequest): EmbeddingResponse =
                EmbeddingResponse(
                    request.instructions.mapIndexed { idx, _ ->
                        Embedding(FloatArray(dims) { 0.0f }, idx)
                    },
                )
        }

    @Bean
    @Primary
    fun testDynamicModelService(
        fakeEmbeddingModel: EmbeddingModel,
        toolCallingManager: ToolCallingManager,
    ): DynamicModelService = FakeDynamicModelService(fakeEmbeddingModel, toolCallingManager)

    class FakeDynamicModelService(
        private val fakeEmbeddingModel: EmbeddingModel,
        toolCallingManager: ToolCallingManager,
    ) : DynamicModelService(
            toolCallingManager = toolCallingManager,
            retryTemplate = RetryTemplate(),
            observationRegistry = ObservationRegistry.create(),
            agentRepo = mockk(relaxed = true),
        ) {
        override fun getEmbeddingModel(agentId: UUID): EmbeddingModel = fakeEmbeddingModel
    }
}
