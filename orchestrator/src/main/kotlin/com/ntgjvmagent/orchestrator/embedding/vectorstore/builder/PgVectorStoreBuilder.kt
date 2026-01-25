package com.ntgjvmagent.orchestrator.embedding.vectorstore.builder

import com.ntgjvmagent.orchestrator.config.EmbeddingProperties
import com.ntgjvmagent.orchestrator.embedding.vectorstore.VectorStoreType
import com.ntgjvmagent.orchestrator.embedding.vectorstore.pgvector.PgVectorSchema
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.ai.vectorstore.pgvector.PgVectorStore
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class PgVectorStoreBuilder(
    private val jdbcTemplate: JdbcTemplate,
    private val embeddingProps: EmbeddingProperties,
) : VectorStoreBuilder {
    override fun supports(type: VectorStoreType): Boolean = type == VectorStoreType.PGVECTOR

    override fun build(embeddingModel: EmbeddingModel): VectorStore =
        PgVectorStore
            .builder(jdbcTemplate, embeddingModel)
            .schemaName("public")
            .vectorTableName(PgVectorSchema.TABLE_NAME)
            .dimensions(embeddingProps.dimension)
            .build()
}
