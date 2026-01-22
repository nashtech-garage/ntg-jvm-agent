package com.ntgjvmagent.orchestrator.embedding.vectorstore.pgvector

/**
 * Vector store schema version.
 *
 * Bump this ONLY when:
 * - embedding model changes
 * - embedding dimension changes
 * - distance metric changes
 *
 * Requires full re-embedding.
 */
object PgVectorSchema {
    const val VERSION = 1
    const val TABLE_NAME = "vector_store_v$VERSION"
    const val EMBEDDING_COLUMN = "embedding"
}
