package com.ntgjvmagent.orchestrator.embedding.vectorstore

interface VectorStoreBackend {
    /** e.g. PGVECTOR, QDRANT, PINECONE */
    val type: VectorStoreType

    /** Initialize storage if needed (schema, collection, index) */
    fun initialize()

    /** Verify compatibility with embedding config */
    fun verifyDimension(expected: Int)
}
