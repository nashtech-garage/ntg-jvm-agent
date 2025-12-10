package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.repository.AgentRepository
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.ai.vectorstore.pgvector.PgVectorStore
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class VectorStoreService(
    private val dynamicModelService: DynamicModelService,
    private val jdbcTemplate: JdbcTemplate,
    private val agentRepo: AgentRepository,
) {
    private val cache = ConcurrentHashMap<UUID, VectorStore>()

    fun getVectorStore(agentId: UUID): VectorStore = cache.computeIfAbsent(agentId) { buildVectorStore(it) }

    fun invalidate(agentId: UUID) {
        cache.remove(agentId)
    }

    private fun buildVectorStore(agentId: UUID): VectorStore {
        // --- Get agent dimension (never call remote embedding API) ---
        val agent =
            agentRepo
                .findById(agentId)
                .orElseThrow { IllegalArgumentException("Agent $agentId not found") }

        val dimension = agent.dimension

        // --- Build embedding model normally (does NOT call API) ---
        val embeddingModel = dynamicModelService.getEmbeddingModel(agentId)

        // --- Build vector store for that agent ---
        return PgVectorStore
            .builder(jdbcTemplate, embeddingModel)
            .dimensions(dimension)
            .schemaName("public")
            .vectorTableName("vector_store_$dimension")
            .build()
    }
}
