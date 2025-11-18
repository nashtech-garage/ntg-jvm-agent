package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.utils.Constant
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

        return PgVectorStore
            .builder(jdbcTemplate, embeddingModel)
            .dimensions(Constant.DIMENSION)
            .schemaName("public")
            .vectorTableName("vector_store")
            .build()
    }
}
