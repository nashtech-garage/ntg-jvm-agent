package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.embedding.runtime.EmbeddingService
import com.ntgjvmagent.orchestrator.embedding.vectorstore.VectorStoreBackend
import com.ntgjvmagent.orchestrator.embedding.vectorstore.builder.VectorStoreBuilder
import org.slf4j.LoggerFactory
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service

@Service
class VectorStoreService(
    private val backend: VectorStoreBackend,
    private val builders: List<VectorStoreBuilder>,
    private val embeddingService: EmbeddingService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Volatile
    private var vectorStore: VectorStore? = null

    /** Global singleton VectorStore */
    fun getVectorStore(): VectorStore =
        vectorStore ?: synchronized(this) {
            vectorStore ?: buildVectorStore().also { vectorStore = it }
        }

    // ---------------------------------------------------------------------

    private fun buildVectorStore(): VectorStore {
        require(vectorStore == null) {
            "VectorStore already initialized; embedding config must be immutable"
        }

        log.info("Building VectorStore for backend {}", backend.type)

        val embeddingModel = embeddingService.embeddingModel()

        val builder =
            builders.firstOrNull { it.supports(backend.type) }
                ?: error("No VectorStoreBuilder found for backend ${backend.type}")

        return builder.build(embeddingModel)
    }
}
