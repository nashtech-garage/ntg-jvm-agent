package com.ntgjvmagent.orchestrator.embedding.vectorstore.builder

import com.ntgjvmagent.orchestrator.embedding.vectorstore.VectorStoreType
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.vectorstore.VectorStore

interface VectorStoreBuilder {
    fun supports(type: VectorStoreType): Boolean

    /** Build a VectorStore instance. */
    fun build(embeddingModel: EmbeddingModel): VectorStore
}
