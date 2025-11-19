package com.ntgjvmagent.orchestrator.service

import org.springframework.ai.vectorstore.pgvector.PgVectorStore
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VectorStoreService(
    private val dynamicModelFactory: DynamicModelFactory,
    private val jdbcTemplate: JdbcTemplate,
) {
    fun getVectorStore(agentId: UUID): PgVectorStore {
        val embeddingModel = dynamicModelFactory.getEmbeddingModel(agentId)
        val dimension = embeddingModel.dimensions()
        return PgVectorStore
            .builder(jdbcTemplate, embeddingModel)
            .dimensions(dimension)
            .schemaName("public")
            .vectorTableName("vector_store_$dimension")
            .build()
    }
}
