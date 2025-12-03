package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.dto.response.KnowledgeChunkResponseDto
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.KnowledgeChunk
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
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
    private val dynamicModelService: DynamicModelService,
) {
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
        val embedding = dynamicModelService.getEmbeddingModel(agentId).embed(content)

        embedding.size
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

        require(chunk.knowledge.id == knowledgeId) { "Chunk $chunkId does not belong to knowledge $knowledgeId" }

        if (chunk.knowledge.agent.id != agentId) {
            throw EntityNotFoundException("Chunk $chunkId does not belong to agent $agentId")
        }

        val embedding = dynamicModelService.getEmbeddingModel(agentId).embed(newContent)
        chunk.apply {
            content = newContent
            metadata = newMetadata
            when (embedding.size) {
                Constant.GEMINI_DIMENSION -> embedding768 = embedding
                Constant.CHATGPT_DIMENSION -> embedding1536 = embedding
                else -> throw IllegalArgumentException("Unsupported embedding dimension: ${embedding.size}")
            }
        }

        vectorStoreService
            .getVectorStore(agentId)
            .add(listOf(buildDocument(chunk)))
        return KnowledgeChunkResponseDto.from(chunkRepo.save(chunk))
    }

    @Transactional(readOnly = true)
    fun getByKnowledge(
        agentId: UUID,
        knowledgeId: UUID,
    ): List<KnowledgeChunkResponseDto> =
        chunkRepo
            .findAllByKnowledgeIdAndKnowledgeAgentIdOrderByChunkOrderAsc(knowledgeId, agentId)
            .map(KnowledgeChunkResponseDto::from)

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
        // Ensure knowledge belongs to agent
        if (!knowledgeRepo.existsByIdAndAgentId(knowledgeId, agentId)) {
            throw EntityNotFoundException("Knowledge $knowledgeId not found for agent $agentId")
        }

        val results: List<Document> =
            vectorStoreService
                .getVectorStore(
                    agentId,
                ).similaritySearch(query)
                .toList()

        if (results.isEmpty()) {
            return emptyList()
        }

        val activeKnowledgeIds = chunkRepo.findAllKnowledgeIdsActiveByAgent(agentId).map { it.toString() }

        return results
            .filter { doc -> doc.metadata["knowledge_id"]?.toString() in activeKnowledgeIds }
            .take(topK)
            .map(KnowledgeChunkResponseDto::fromDocument)
    }

    private fun buildDocument(chunk: KnowledgeChunk): Document {
        val metadata =
            chunk.metadata +
                mapOf(
                    "chunk_id" to chunk.id.toString(),
                    "knowledge_id" to chunk.knowledge.id.toString(),
                    "chunk_order" to chunk.chunkOrder.toString(),
                )
        return Document(chunk.id.toString(), chunk.content, metadata)
    }
}
