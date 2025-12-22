package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.dto.response.KnowledgeChunkResponseDto
import com.ntgjvmagent.orchestrator.embedding.EmbeddingQueueService
import com.ntgjvmagent.orchestrator.embedding.EmbeddingService
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.KnowledgeChunk
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.repository.EmbeddingJobRepository
import com.ntgjvmagent.orchestrator.repository.KnowledgeChunkRepository
import com.ntgjvmagent.orchestrator.utils.Constant
import jakarta.persistence.EntityNotFoundException
import org.springframework.ai.document.Document
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class KnowledgeChunkService(
    private val chunkRepo: KnowledgeChunkRepository,
    private val knowledgeRepo: AgentKnowledgeRepository,
    private val vectorStoreService: VectorStoreService,
    private val embeddingService: EmbeddingService,
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
                ?: throw EntityNotFoundException("Knowledge $knowledgeId not found for agent $agentId")

        val order = chunkOrder ?: getNextChunkOrderForKnowledge(agentId, knowledgeId)

        val chunk =
            KnowledgeChunk(
                knowledge = knowledge,
                chunkOrder = order,
                content = content,
                metadata = metadata,
                embedding768 = null,
                embedding1536 = null,
            )

        val savedChunk = chunkRepo.save(chunk)

        embeddingQueueService.enqueueForChunk(savedChunk)

        return KnowledgeChunkResponseDto.from(savedChunk)
    }

    @Transactional
    fun addChunk(
        agentId: UUID,
        knowledgeId: UUID,
        content: String,
        metadata: Map<String, Any?> = emptyMap(),
        chunkOrder: Int? = null,
    ): KnowledgeChunkResponseDto {
        val knowledge =
            knowledgeRepo.findByIdAndAgentId(knowledgeId, agentId)
                ?: throw EntityNotFoundException("Knowledge $knowledgeId not found for agent $agentId")

        val order = chunkOrder ?: getNextChunkOrderForKnowledge(agentId, knowledgeId)

        val correlationId = "knowledge-$knowledgeId:add-chunk:$order"

        val embedding =
            embeddingService.embed(
                agentId = agentId,
                text = content,
                correlationId = correlationId,
            )

        val chunk =
            KnowledgeChunk(
                knowledge = knowledge,
                chunkOrder = order,
                content = content,
                metadata = metadata,
                embedding768 = if (embedding.size == Constant.GEMINI_DIMENSION) embedding else null,
                embedding1536 = if (embedding.size == Constant.CHATGPT_DIMENSION) embedding else null,
            )

        val savedChunk = chunkRepo.save(chunk)

        vectorStoreService
            .getVectorStore(agentId)
            .add(listOf(buildDocument(savedChunk)))

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

        val correlationId = "knowledge-$knowledgeId:update-chunk:$chunkId"

        val embedding =
            embeddingService.embed(
                agentId = agentId,
                text = newContent,
                correlationId = correlationId,
            )

        chunk.apply {
            content = newContent
            metadata = newMetadata
            when (embedding.size) {
                Constant.GEMINI_DIMENSION -> embedding768 = embedding
                Constant.CHATGPT_DIMENSION -> embedding1536 = embedding
                else -> throw IllegalArgumentException("Unsupported embedding dimension: ${embedding.size}")
            }
        }

        val savedChunk = chunkRepo.save(chunk)

        vectorStoreService
            .getVectorStore(agentId)
            .add(listOf(buildDocument(savedChunk)))

        return KnowledgeChunkResponseDto.from(savedChunk)
    }

    @Transactional(readOnly = true)
    fun getByKnowledge(
        agentId: UUID,
        knowledgeId: UUID,
    ): List<KnowledgeChunkResponseDto> =
        chunkRepo
            .findAllByKnowledgeIdAndKnowledgeAgentIdOrderByChunkOrderAsc(knowledgeId, agentId)
            .map(KnowledgeChunkResponseDto.Companion::from)

    @Transactional(readOnly = true)
    fun countByKnowledge(
        agentId: UUID,
        knowledgeId: UUID,
    ): Long = chunkRepo.countByKnowledgeIdAndKnowledgeAgentId(knowledgeId, agentId)

    @Transactional(readOnly = true)
    fun getNextChunkOrderForKnowledge(
        agentId: UUID,
        knowledgeId: UUID,
    ): Int {
        val currentOrder = chunkRepo.findMaxChunkOrderByKnowledgeIdAndAgentId(knowledgeId, agentId) ?: 0
        return currentOrder + 1
    }

    @Transactional(readOnly = true)
    fun searchSimilarChunks(
        agentId: UUID,
        knowledgeId: UUID,
        query: String,
        topK: Int = 5,
    ): List<KnowledgeChunkResponseDto> {
        if (!knowledgeRepo.existsByIdAndAgentId(knowledgeId, agentId)) {
            throw EntityNotFoundException("Knowledge $knowledgeId not found for agent $agentId")
        }

        val results =
            vectorStoreService.getVectorStore(agentId).similaritySearch(query).toList()

        if (results.isEmpty()) return emptyList()

        val activeKnowledgeIds =
            chunkRepo.findAllKnowledgeIdsActiveByAgent(agentId).map { it.toString() }

        return results
            .filter { doc -> doc.metadata["knowledgeId"]?.toString() in activeKnowledgeIds }
            .take(topK)
            .map(KnowledgeChunkResponseDto.Companion::fromDocument)
    }

    private fun buildDocument(chunk: KnowledgeChunk): Document {
        val metadata =
            chunk.metadata +
                mapOf(
                    "chunkId" to chunk.id.toString(),
                    "knowledgeId" to chunk.knowledge.id.toString(),
                    "chunkOrder" to chunk.chunkOrder,
                )

        return Document(chunk.id.toString(), chunk.content, metadata)
    }

    @Transactional
    fun deleteAllChunks(
        agentId: UUID,
        knowledgeId: UUID,
    ) {
        val chunks =
            chunkRepo.findAllByKnowledgeIdAndKnowledgeAgentIdOrderByChunkOrderAsc(knowledgeId, agentId)

        if (chunks.isEmpty()) return

        val chunkIds = chunks.map { it.id!! }
        val vectorIds = chunkIds.map(UUID::toString)

        // 1. Delete embeddings from vector store
        vectorStoreService
            .getVectorStore(agentId)
            .delete(vectorIds)

        // 2. Delete embedding jobs for these chunks
        embeddingJobRepo.deleteAllByChunkIds(chunkIds)

        // 3. Delete chunks
        chunkRepo.deleteAll(chunks)
    }
}
