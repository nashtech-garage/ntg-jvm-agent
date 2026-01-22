package com.ntgjvmagent.orchestrator.embedding.vectorstore.pgvector

import com.ntgjvmagent.orchestrator.config.EmbeddingProperties
import com.ntgjvmagent.orchestrator.embedding.vectorstore.VectorStoreBackend
import com.ntgjvmagent.orchestrator.embedding.vectorstore.VectorStoreType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "embedding.vector-store",
    name = ["type"],
    havingValue = "pgvector",
    matchIfMissing = true, // pgvector is default
)
class PgVectorBackend(
    @Qualifier("vectorJdbcTemplate")
    private val jdbcTemplate: JdbcTemplate,
    private val embeddingProps: EmbeddingProperties,
) : VectorStoreBackend {
    private val log = LoggerFactory.getLogger(javaClass)

    override val type: VectorStoreType = VectorStoreType.PGVECTOR

    override fun initialize() {
        log.info("Initializing pgvector backend")

        ensureExtension()
        ensureTable()
        ensureIndex()
    }

    override fun verifyDimension(expected: Int) {
        val actual = readVectorDimension()

        when {
            actual == 0 -> {
                log.info(
                    "pgvector table '{}' is empty/uninitialized (dimension=0). " +
                        "Configured dimension is {}. Proceeding.",
                    PgVectorSchema.TABLE_NAME,
                    expected,
                )
            }

            actual != expected -> {
                error(
                    """
                    Vector store dimension mismatch:
                      - Backend:     PGVECTOR
                      - Table:       ${PgVectorSchema.TABLE_NAME}
                      - Configured:  $expected
                      - Database:    $actual

                    This requires a full re-embedding.
                    Bump VECTOR_TABLE_NAME and restart.
                    """.trimIndent(),
                )
            }

            else -> {
                log.info(
                    "pgvector table '{}' dimension verified: {}",
                    PgVectorSchema.TABLE_NAME,
                    actual,
                )
            }
        }
    }

    // ---------------------------------------------------------------------
    // pgvector-specific internals
    // ---------------------------------------------------------------------

    private fun ensureExtension() {
        jdbcTemplate.execute(
            """
            CREATE EXTENSION IF NOT EXISTS vector
            """.trimIndent(),
        )
    }

    private fun ensureTable() {
        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS ${PgVectorSchema.TABLE_NAME} (
                id UUID PRIMARY KEY,
                content TEXT,
                ${PgVectorSchema.EMBEDDING_COLUMN} VECTOR(${embeddingProps.dimension}),
                metadata JSONB
            )
            """.trimIndent(),
        )
    }

    private fun ensureIndex() {
        jdbcTemplate.execute(
            """
            CREATE INDEX IF NOT EXISTS idx_${PgVectorSchema.TABLE_NAME}_embedding
            ON ${PgVectorSchema.TABLE_NAME}
            USING ivfflat (${PgVectorSchema.EMBEDDING_COLUMN} vector_cosine_ops)
            """.trimIndent(),
        )
    }

    /**
     * Read pgvector dimension using pg_catalog.
     *
     * Semantics:
     * - 0 → column exists but no vectors written yet (VALID)
     * - >0 → fixed dimension
     */
    private fun readVectorDimension(): Int =
        jdbcTemplate.queryForObject(
            """
            SELECT attndims
            FROM pg_attribute
            WHERE attrelid = '${PgVectorSchema.TABLE_NAME}'::regclass
              AND attname = '${PgVectorSchema.EMBEDDING_COLUMN}'
              AND NOT attisdropped
            """.trimIndent(),
            Int::class.java,
        ) ?: 0
}
