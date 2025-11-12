package com.ntgjvmagent.orchestrator.integration.config

import org.springframework.ai.document.Document
import org.springframework.ai.embedding.BatchingStrategy
import org.springframework.ai.embedding.Embedding
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.embedding.EmbeddingOptions
import org.springframework.ai.embedding.EmbeddingRequest
import org.springframework.ai.embedding.EmbeddingResponse
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TestEmbeddingConfig {
    companion object {
        const val DUMMY_DIMENSIONS = 1536
    }

    @Bean
    @Primary
    fun fakeEmbeddingModel(): EmbeddingModel =
        object : EmbeddingModel {
            override fun embed(text: String): FloatArray = FloatArray(DUMMY_DIMENSIONS) { 0.0f }

            override fun embed(texts: List<String>): List<FloatArray> =
                texts.map { FloatArray(DUMMY_DIMENSIONS) { 0.0f } }

            override fun embed(document: Document): FloatArray = FloatArray(DUMMY_DIMENSIONS) { 0.0f }

            override fun embed(
                documents: MutableList<Document>,
                options: EmbeddingOptions,
                batchingStrategy: BatchingStrategy,
            ): MutableList<FloatArray> = documents.map { FloatArray(DUMMY_DIMENSIONS) { 0.0f } }.toMutableList()

            override fun call(request: EmbeddingRequest): EmbeddingResponse {
                val inputs = request.instructions
                val embeddings: List<Embedding> =
                    inputs.mapIndexed { idx, _ ->
                        Embedding(FloatArray(DUMMY_DIMENSIONS) { 0.0f }, idx)
                    }
                return EmbeddingResponse(embeddings)
            }
        }
}
