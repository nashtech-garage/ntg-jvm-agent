package com.ntgjvmagent.orchestrator.integration.tool

import com.ntgjvmagent.orchestrator.config.ToolSearchIndexProperties
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.service.VectorStoreService
import com.ntgjvmagent.orchestrator.tool.SessionScopedVectorToolIndex
import com.ntgjvmagent.orchestrator.tool.maintenance.ToolIndexCleanupJob
import com.ntgjvmagent.orchestrator.unit.support.BagOfWordsEmbeddingModel
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.ai.document.Document
import org.springframework.ai.tool.toolsearch.ToolReference
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.ai.vectorstore.pgvector.PgVectorStore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals

/**
 * The sweep is one filtered delete against the store the RAG chunks live in, and filter
 * semantics differ between backends: SimpleVectorStore treats a comparison against an absent
 * metadata key as true, which without the marker term would delete every chunk. This runs the
 * production filter against real pgvector, on the image the app deploys against.
 */
class ToolIndexCleanupPgVectorIT
    @Autowired
    constructor(
        private val jdbcTemplate: JdbcTemplate,
    ) : BaseIntegrationTest() {
        private val now = Instant.parse("2026-06-01T00:00:00Z")

        @BeforeEach
        fun createVectorTable() {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector")
            jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS $TABLE (
                    id UUID PRIMARY KEY,
                    content TEXT,
                    embedding VECTOR($DIMS),
                    metadata JSONB
                )
                """.trimIndent(),
            )
            jdbcTemplate.execute("TRUNCATE TABLE $TABLE")
        }

        @Test
        fun `stale tool documents are swept and knowledge chunks are left untouched`() {
            val store = pgVectorStore()
            indexTool(store, at = now.minus(Duration.ofDays(31)), session = "stale", tool = "refundOrder")
            indexTool(store, at = now.minus(Duration.ofDays(29)), session = "fresh", tool = "refundInvoice")
            store.add(
                listOf(
                    Document(
                        UUID.randomUUID().toString(),
                        "refund window is thirty days",
                        mapOf("knowledgeId" to UUID.randomUUID().toString()),
                    ),
                ),
            )

            job(store).cleanup()

            assertEquals(listOf("refundInvoice"), remainingToolNames())
            assertEquals(1, remainingChunkCount())
        }

        private fun job(store: VectorStore) =
            ToolIndexCleanupJob(
                vectorStoreService = mockk<VectorStoreService> { every { getVectorStore() } returns store },
                properties = ToolSearchIndexProperties(documentTtl = Duration.ofDays(30)),
                clock = Clock.fixed(now, ZoneOffset.UTC),
            )

        private fun indexTool(
            store: VectorStore,
            at: Instant,
            session: String,
            tool: String,
        ) = SessionScopedVectorToolIndex(store, Clock.fixed(at, ZoneOffset.UTC))
            .indexTool(
                session,
                ToolReference
                    .builder()
                    .toolName(tool)
                    .summary("Refund something")
                    .build(),
            )

        private fun pgVectorStore(): VectorStore =
            PgVectorStore
                .builder(jdbcTemplate, BagOfWordsEmbeddingModel)
                .schemaName("public")
                .vectorTableName(TABLE)
                .dimensions(DIMS)
                .initializeSchema(false)
                .build()

        private fun remainingToolNames(): List<String> =
            jdbcTemplate
                .queryForList(
                    "SELECT metadata->>'${SessionScopedVectorToolIndex.TOOL_NAME_METADATA}' AS name FROM $TABLE " +
                        "WHERE metadata ? '${SessionScopedVectorToolIndex.INDEX_MARKER_METADATA}'",
                ).map { it["name"] as String }

        private fun remainingChunkCount(): Int =
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM $TABLE WHERE metadata ? 'knowledgeId'",
                Int::class.java,
            ) ?: 0

        private companion object {
            const val TABLE = "vector_store_tool_cleanup_it"
            const val DIMS = 64
        }
    }
