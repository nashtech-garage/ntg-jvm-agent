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
        knowledgeId: UUID,
        content: String,
        metadata: Map<String, Any>? = null,
        chunkOrder: Int? = null,
    ): KnowledgeChunkResponseDto {
        val knowledge =
            knowledgeRepo.findByIdOrNull(knowledgeId)
                ?: throw EntityNotFoundException("Knowledge not found: $knowledgeId")

        val order = chunkOrder ?: getNextChunkOrderForKnowledge(knowledgeId)
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
        chunkId: UUID,
        newContent: String,
        newMetadata: Map<String, Any>? = null,
    ): KnowledgeChunkResponseDto {
        val chunk =
            chunkRepo.findByIdOrNull(chunkId)
                ?: throw EntityNotFoundException("Chunk not found: $chunkId")

        chunk.content = newContent
        chunk.metadata = newMetadata
        chunk.embedding = embeddingModel.embed(newContent)

        vectorStore.add(listOf(buildDocument(chunk)))
        return KnowledgeChunkResponseDto.from(chunk)
    }

    @Transactional(readOnly = true)
    fun getByKnowledge(knowledgeId: UUID): List<KnowledgeChunkResponseDto> =
        chunkRepo.findAllByKnowledgeId(knowledgeId).map {
            KnowledgeChunkResponseDto.from(it)
        }

    @Transactional(readOnly = true)
    fun countByKnowledge(knowledgeId: UUID): Long = chunkRepo.countByKnowledgeId(knowledgeId)

    @Transactional(readOnly = true)
    fun getNextChunkOrderForKnowledge(knowledgeId: UUID): Int {
        val currentOrder = chunkRepo.findMaxChunkOrderByKnowledgeId(knowledgeId) ?: 0
        return currentOrder + 1
    }

    @Transactional(readOnly = true)
    fun searchSimilarChunks(
        query: String,
        topK: Int = 5,
    ): List<KnowledgeChunkResponseDto> {
        val activeKnowledgeIds = chunkRepo.findAllKnowledgeIdsActive().map { it.toString() }
        val results: List<Document> = vectorStore.similaritySearch(query).toList()

        return results
            .filter { doc ->
                val knowledgeId = doc.metadata["knowledge_id"]?.toString()
                knowledgeId != null && activeKnowledgeIds.contains(knowledgeId)
            }.take(topK)
            .map { KnowledgeChunkResponseDto.fromDocument(it) }
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
