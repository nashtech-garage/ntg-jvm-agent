package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.dto.response.KnowledgeChunkResponseDto
import com.ntgjvmagent.orchestrator.embedding.queue.EmbeddingQueueService
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.KnowledgeChunk
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.repository.EmbeddingJobRepository
import com.ntgjvmagent.orchestrator.repository.KnowledgeChunkRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class KnowledgeChunkService(
    private val chunkRepo: KnowledgeChunkRepository,
    private val knowledgeRepo: AgentKnowledgeRepository,
    private val vectorStoreService: VectorStoreService,
    private val embeddingQueueService: EmbeddingQueueService,
    private val embeddingJobRepo: EmbeddingJobRepository,
) {
    @Transactional
    fun createChunkAndEnqueueEmbedding(
        agentId: UUID,
        knowledgeId: UUID,
        content: String,
        metadata: Map<String, Any?> = emptyMap(),
        chunkOrder: Int? = null,
    ): KnowledgeChunkResponseDto {
        val knowledge =
            knowledgeRepo.findByIdAndAgentId(knowledgeId, agentId)
                ?: throw EntityNotFoundException(
                    "Knowledge $knowledgeId not found for agent $agentId",
                )

        val order = chunkOrder ?: getNextChunkOrderForKnowledge(agentId, knowledgeId)

        val chunk =
            KnowledgeChunk(
                knowledge = knowledge,
                chunkOrder = order,
                content = content,
                metadata = metadata,
            )

        val savedChunk = chunkRepo.save(chunk)

        // Async embedding → vector store
        embeddingQueueService.enqueueForChunk(savedChunk)

        return KnowledgeChunkResponseDto.from(savedChunk)
    }

    @Transactional
    fun updateChunk(
        agentId: UUID,
        knowledgeId: UUID,
        chunkId: UUID,
        newContent: String,
        newMetadata: Map<String, Any?> = emptyMap(),
    ): KnowledgeChunkResponseDto {
        val chunk =
            chunkRepo.findByIdOrNull(chunkId)
                ?: throw EntityNotFoundException("Chunk $chunkId not found")

        require(chunk.knowledge.id == knowledgeId)
        require(chunk.knowledge.agent.id == agentId)

        chunk.apply {
            content = newContent
            metadata = newMetadata
        }

        val savedChunk = chunkRepo.save(chunk)

        // Re-embed asynchronously
        embeddingQueueService.enqueueForChunk(savedChunk)

        return KnowledgeChunkResponseDto.from(savedChunk)
    }

    @Transactional(readOnly = true)
    fun getByKnowledge(
        agentId: UUID,
        knowledgeId: UUID,
    ): List<KnowledgeChunkResponseDto> =
        chunkRepo
            .findAllByKnowledgeIdAndKnowledgeAgentIdOrderByChunkOrderAsc(
                knowledgeId,
                agentId,
            ).map(KnowledgeChunkResponseDto::from)

    @Transactional(readOnly = true)
    fun countByKnowledge(
        agentId: UUID,
        knowledgeId: UUID,
    ): Long = chunkRepo.countByKnowledgeIdAndKnowledgeAgentId(knowledgeId, agentId)

    @Transactional(readOnly = true)
    fun getNextChunkOrderForKnowledge(
        agentId: UUID,
        knowledgeId: UUID,
    ): Int = (chunkRepo.findMaxChunkOrderByKnowledgeIdAndAgentId(knowledgeId, agentId) ?: 0) + 1

    @Transactional(readOnly = true)
    fun searchSimilarChunks(
        agentId: UUID,
        knowledgeId: UUID,
        query: String,
        topK: Int = 5,
    ): List<KnowledgeChunkResponseDto> {
        if (!knowledgeRepo.existsByIdAndAgentId(knowledgeId, agentId)) {
            throw EntityNotFoundException(
                "Knowledge $knowledgeId not found for agent $agentId",
            )
        }

        val results =
            vectorStoreService
                .getVectorStore()
                .similaritySearch(query)
                .take(topK)

        if (results.isEmpty()) return emptyList()

        val activeKnowledgeIds =
            chunkRepo
                .findAllKnowledgeIdsActiveByAgent(agentId)
                .map(UUID::toString)
                .toSet()

        return results
            .filter { it.metadata["knowledgeId"] in activeKnowledgeIds }
            .map(KnowledgeChunkResponseDto::fromDocument)
    }

    @Transactional
    fun deleteAllChunks(
        agentId: UUID,
        knowledgeId: UUID,
    ) {
        val chunks =
            chunkRepo.findAllByKnowledgeIdAndKnowledgeAgentIdOrderByChunkOrderAsc(
                knowledgeId,
                agentId,
            )

        if (chunks.isEmpty()) return

        val chunkIds = chunks.mapNotNull { it.id }
        val vectorIds = chunkIds.map(UUID::toString)

        // 1️ Remove from vector store
        vectorStoreService.getVectorStore().delete(vectorIds)

        // 2️ Remove embedding jobs
        embeddingJobRepo.deleteAllByChunkIds(chunkIds)

        // 3️ Remove chunks
        chunkRepo.deleteAll(chunks)
    }
}
