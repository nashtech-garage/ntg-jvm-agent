package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.repository.AgentRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.ai.vectorstore.pgvector.PgVectorStore
import org.springframework.data.repository.findByIdOrNull
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

    /**
     * Return cached vector store or build it atomically if missing.
     */
    fun getVectorStore(agentId: UUID): VectorStore = cache.computeIfAbsent(agentId) { buildVectorStore(it) }

    /**
     * Atomically reload vector store for this agent.
     * Ensures only one thread rebuilds the store.
     */
    fun reload(agentId: UUID) {
        cache.compute(agentId) { _, _ ->
            buildVectorStore(agentId)
        }
    }

    /**
     * Preferred replacement for invalidate() — because remove() creates
     * a race window in high concurrency systems.
     */
    fun invalidate(agentId: UUID) {
        reload(agentId)
    }

    /**
     * Build the vector store for the given agent.
     */
    private fun buildVectorStore(agentId: UUID): VectorStore {
        // Get agent config (local DB)
        val agent =
            agentRepo.findByIdOrNull(agentId)
                ?: throw EntityNotFoundException("Agent $agentId not found")

        val dimension = agent.dimension

        // PgVectorStore requires the BLOCKING Spring AI EmbeddingModel
        val springEmbeddingModel = dynamicModelService.getRawSpringEmbeddingModel(agentId)

        // Build vector store with per-agent table
        return PgVectorStore
            .builder(jdbcTemplate, springEmbeddingModel)
            .dimensions(dimension)
            .schemaName("public")
            .vectorTableName("vector_store_$dimension")
            .build()
    }
}
