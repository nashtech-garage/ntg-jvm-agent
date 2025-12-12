package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.chunking.DocumentChunker
import com.ntgjvmagent.orchestrator.exception.BadRequestException
import com.ntgjvmagent.orchestrator.model.KnowledgeStatus
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Service
class KnowledgeImportService(
    private val chunkService: KnowledgeChunkService,
    private val knowledgeRepo: AgentKnowledgeRepository,
    private val documentChunker: DocumentChunker,
) {
    private val logger = LoggerFactory.getLogger(KnowledgeImportService::class.java)

    @Transactional
    fun performImport(
        agentId: UUID,
        knowledgeId: UUID,
        file: MultipartFile,
    ): Int {
        val fileName = file.originalFilename ?: "uploaded-file"
        val contentType = file.contentType ?: "application/octet-stream"

        logger.info(
            "Worker importing FILE for agent={}, knowledge={}, name={}, contentType={}, size={} bytes",
            agentId,
            knowledgeId,
            fileName,
            contentType,
            file.size,
        )

        val knowledge =
            knowledgeRepo.findByIdAndAgentId(knowledgeId, agentId)
                ?: throw EntityNotFoundException("Knowledge $knowledgeId not found for agent $agentId")

        val documents = documentChunker.chunkFile(file)
        if (documents.isEmpty()) {
            throw BadRequestException("File contains no readable text: $fileName")
        }

        var order = chunkService.getNextChunkOrderForKnowledge(agentId, knowledgeId)

        documents.forEachIndexed { index, doc ->

            val enrichedMetadata =
                doc.metadata +
                    mapOf(
                        "fileName" to fileName,
                        "fileOrder" to index,
                        "importTimestamp" to System.currentTimeMillis(),
                    )

            chunkService.createChunkAndEnqueueEmbedding(
                agentId = agentId,
                knowledgeId = knowledgeId,
                content = doc.text ?: "",
                metadata = enrichedMetadata,
                chunkOrder = order,
            )

            order++
        }

        knowledge.status = KnowledgeStatus.EMBEDDING_PENDING
        knowledge.errorMessage = null
        knowledgeRepo.save(knowledge)

        logger.info(
            "Worker finished FILE import: agent={}, knowledge={}, file={}, chunksCreated={}",
            agentId,
            knowledgeId,
            fileName,
            documents.size,
        )

        return documents.size
    }
}
