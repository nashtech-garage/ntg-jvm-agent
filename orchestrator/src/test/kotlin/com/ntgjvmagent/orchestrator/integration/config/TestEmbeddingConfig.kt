package com.ntgjvmagent.orchestrator.integration.config

import com.ntgjvmagent.orchestrator.chat.ChatModelOrchestrator
import com.ntgjvmagent.orchestrator.embedding.runtime.ReactiveEmbeddingModel
import com.ntgjvmagent.orchestrator.embedding.runtime.adapter.SpringAiEmbeddingModelAdapter
import com.ntgjvmagent.orchestrator.embedding.vectorstore.VectorStoreBackend
import com.ntgjvmagent.orchestrator.embedding.vectorstore.VectorStoreType
import com.ntgjvmagent.orchestrator.embedding.vectorstore.builder.VectorStoreBuilder
import io.mockk.every
import io.mockk.mockk
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.document.Document
import org.springframework.ai.embedding.BatchingStrategy
import org.springframework.ai.embedding.Embedding
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.embedding.EmbeddingOptions
import org.springframework.ai.embedding.EmbeddingRequest
import org.springframework.ai.embedding.EmbeddingResponse
import org.springframework.ai.model.tool.ToolCallingManager
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
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
    // 2) Fake VectorStore (CORE TEST SEAM)
    // ---------------------------------------------------------------------
    @Bean
    @Primary
    fun fakeVectorStore(): VectorStore =
        mockk {
            every { add(any<List<Document>>()) } returns Unit
            every { delete(any<List<String>>()) } returns Unit
            every { similaritySearch(any<String>()) } returns emptyList()
        }

    // ---------------------------------------------------------------------
    // 3) Fake VectorStoreBackend (NO-OP lifecycle)
    // ---------------------------------------------------------------------
    @Bean
    @Primary
    fun fakeVectorStoreBackend(): VectorStoreBackend =
        object : VectorStoreBackend {
            override val type = VectorStoreType.FAKE

            override fun initialize() {}

            override fun verifyDimension(expected: Int) {}
        }

    // ---------------------------------------------------------------------
    // 4) Fake VectorStoreBuilder (returns fake VectorStore)
    // ---------------------------------------------------------------------
    @Bean
    @Primary
    fun fakeVectorStoreBuilder(fakeVectorStore: VectorStore): VectorStoreBuilder =
        object : VectorStoreBuilder {
            override fun supports(type: VectorStoreType): Boolean = type == VectorStoreType.FAKE

            override fun build(embeddingModel: EmbeddingModel): VectorStore = fakeVectorStore
        }

    // ---------------------------------------------------------------------
    // 5) Fake Spring AI EmbeddingModel (GLOBAL)
    // ---------------------------------------------------------------------
    @Bean
    @Primary
    fun fakeSpringEmbeddingModel(): SpringEmbeddingModel =
        object : SpringEmbeddingModel {
            private val dims = 1536

            override fun embed(text: String): FloatArray = FloatArray(dims) { 0.1f }

            override fun embed(texts: List<String>): List<FloatArray> =
                texts
                    .ifEmpty { listOf("dummy") }
                    .map { FloatArray(dims) { 0.1f } }

            override fun embed(document: Document): FloatArray = FloatArray(dims) { 0.1f }

            override fun embed(
                documents: MutableList<Document>,
                options: EmbeddingOptions,
                batchingStrategy: BatchingStrategy,
            ): MutableList<FloatArray> = documents.map { FloatArray(dims) { 0.1f } }.toMutableList()

            override fun call(request: EmbeddingRequest): EmbeddingResponse =
                EmbeddingResponse(
                    request.instructions.mapIndexed { idx, _ ->
                        Embedding(FloatArray(dims) { 0.1f }, idx)
                    },
                )
        }

    // ---------------------------------------------------------------------
    // 6) Fake ReactiveEmbeddingModel
    // ---------------------------------------------------------------------
    @Bean
    @Primary
    fun fakeReactiveEmbeddingModel(springEmbeddingModel: SpringEmbeddingModel): ReactiveEmbeddingModel =
        SpringAiEmbeddingModelAdapter(springEmbeddingModel)

    // ---------------------------------------------------------------------
    // 7) Fake Chat components
    // ---------------------------------------------------------------------
    @Bean
    @Primary
    fun fakeChatModelOrchestrator(): ChatModelOrchestrator = mockk(relaxed = true)

    @Bean
    @Primary
    fun fakeChatModel(): ChatModel = mockk(relaxed = true)
}
