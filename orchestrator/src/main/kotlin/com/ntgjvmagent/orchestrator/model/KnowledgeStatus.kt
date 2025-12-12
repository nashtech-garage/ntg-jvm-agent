package com.ntgjvmagent.orchestrator.model

enum class KnowledgeStatus {
    PENDING,
    INGESTING,
    EMBEDDING_PENDING, // ingestion OK, embedding jobs in queue
    READY, // all embeddings done
    FAILED,
}
