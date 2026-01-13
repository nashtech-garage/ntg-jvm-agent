package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.chunking.DocumentChunker
import com.ntgjvmagent.orchestrator.exception.BadRequestException
import com.ntgjvmagent.orchestrator.model.KnowledgeSourceType
import com.ntgjvmagent.orchestrator.model.KnowledgeStatus
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.storage.core.ObjectStorage
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class KnowledgeImportService(
    private val chunkService: KnowledgeChunkService,
    private val knowledgeRepo: AgentKnowledgeRepository,
    private val documentChunker: DocumentChunker,
    private val objectStorage: ObjectStorage,
) {
    private val logger = LoggerFactory.getLogger(KnowledgeImportService::class.java)

    @Transactional
    fun performImport(
        agentId: UUID,
        knowledgeId: UUID,
    ): Int {
        val knowledge =
            knowledgeRepo.findByIdAndAgentId(knowledgeId, agentId)
                ?: throw EntityNotFoundException(
                    "Knowledge $knowledgeId not found for agent $agentId",
                )

        require(knowledge.sourceType == KnowledgeSourceType.FILE) {
            "performImport only supports FILE knowledge"
        }

        logger.info(
            "Worker importing FILE for agent={}, knowledge={}, name={}, storageKey={}",
            agentId,
            knowledgeId,
            knowledge.name,
            knowledge.storageKey,
        )

        check(objectStorage.exists(knowledge.storageKey!!)) { "Stored file not found for knowledge $knowledgeId" }

        val documents =
            objectStorage.load(knowledge.storageKey!!).use { input ->
                documentChunker.chunk(input, knowledge.originalFileName)
            }

        if (documents.isEmpty()) {
            throw BadRequestException(
                "File contains no readable text: ${knowledge.originalFileName}",
            )
        }

        var order =
            chunkService.getNextChunkOrderForKnowledge(agentId, knowledgeId)

        documents.forEach { doc ->

            val enrichedMetadata =
                doc.metadata +
                    mapOf(
                        "knowledgeName" to knowledge.name,
                        "originalFileName" to knowledge.originalFileName,
                        "importTimestamp" to System.currentTimeMillis(),
                    )

            chunkService.createChunkAndEnqueueEmbedding(
                agentId = agentId,
                knowledgeId = knowledgeId,
                content = doc.text.orEmpty(),
                metadata = enrichedMetadata,
                chunkOrder = order,
            )

            order++
        }

        knowledge.apply {
            status = KnowledgeStatus.EMBEDDING_PENDING
            errorMessage = null
        }

        knowledgeRepo.save(knowledge)

        logger.info(
            "Worker finished FILE import: agent={}, knowledge={}, chunksCreated={}",
            agentId,
            knowledgeId,
            documents.size,
        )

        return documents.size
    }
}
