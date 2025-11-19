package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.dto.KnowledgeChunkResponseDto
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.KnowledgeChunk
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.repository.KnowledgeChunkRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.ai.document.Document
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class KnowledgeChunkService(
    private val chunkRepo: KnowledgeChunkRepository,
    private val knowledgeRepo: AgentKnowledgeRepository,
    private val vectorStore: VectorStore,
    private val embeddingModel: EmbeddingModel,
) {
    @Transactional
    fun addChunk(
        agentId: UUID,
        knowledgeId: UUID,
        content: String,
        metadata: Map<String, Any>? = null,
        chunkOrder: Int? = null,
    ): KnowledgeChunkResponseDto {
        val knowledge =
            knowledgeRepo.findByIdAndAgentId(knowledgeId, agentId)
                ?: throw EntityNotFoundException("Knowledge $knowledgeId not found for agent $agentId")

        val order = chunkOrder ?: getNextChunkOrderForKnowledge(agentId, knowledgeId)
        val embedding = embeddingModel.embed(content)

        val chunk =
            KnowledgeChunk(
                knowledge = knowledge,
                chunkOrder = order,
                content = content,
                metadata = metadata,
                embedding = embedding,
            )

        val savedChunk = chunkRepo.save(chunk)
        vectorStore.add(listOf(buildDocument(savedChunk)))

        return KnowledgeChunkResponseDto.from(savedChunk)
    }

    @Transactional
    fun updateChunk(
        agentId: UUID,
        knowledgeId: UUID,
        chunkId: UUID,
        newContent: String,
        newMetadata: Map<String, Any>? = null,
    ): KnowledgeChunkResponseDto {
        val chunk =
            chunkRepo.findByIdOrNull(chunkId)
                ?: throw EntityNotFoundException("Chunk $chunkId not found")

        require(chunk.knowledge.id == knowledgeId) { "Chunk $chunkId does not belong to knowledge $knowledgeId" }

        if (chunk.knowledge.agent.id != agentId) {
            throw EntityNotFoundException("Chunk $chunkId does not belong to agent $agentId")
        }

        chunk.content = newContent
        chunk.metadata = newMetadata
        chunk.embedding = embeddingModel.embed(newContent)

        vectorStore.add(listOf(buildDocument(chunk)))
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

        val results: List<Document> = vectorStore.similaritySearch(query).toList()

        if (results.isEmpty()) {
            return emptyList()
        }

        val activeKnowledgeIds = chunkRepo.findAllKnowledgeIdsActiveByAgent(agentId).map { it.toString() }

        return results
            .filter { doc ->
                val knowledgeId = doc.metadata["knowledge_id"]?.toString()
                knowledgeId != null && activeKnowledgeIds.contains(knowledgeId)
            }.take(topK)
            .map(KnowledgeChunkResponseDto::fromDocument)
    }

    private fun buildDocument(chunk: KnowledgeChunk): Document {
        val metadata =
            (chunk.metadata ?: emptyMap()) +
                mapOf(
                    "chunk_id" to chunk.id.toString(),
                    "knowledge_id" to chunk.knowledge.id.toString(),
                    "chunk_order" to chunk.chunkOrder.toString(),
                )
        return Document(chunk.id.toString(), chunk.content, metadata)
    }
}
