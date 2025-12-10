package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.chunking.DocumentChunker
import com.ntgjvmagent.orchestrator.exception.BadRequestException
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.viewmodel.KnowledgeImportingResponseVm
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
    fun importDocument(
        agentId: UUID,
        knowledgeId: UUID,
        file: MultipartFile,
    ): KnowledgeImportingResponseVm {
        val fileName = file.originalFilename ?: "uploaded-file"
        val contentType = file.contentType ?: "application/octet-stream"

        logger.info(
            "Importing FILE for agent={}, knowledge={}, name={}, contentType={}, size={} bytes",
            agentId,
            knowledgeId,
            fileName,
            contentType,
            file.size,
        )

        // Validate parent knowledge row
        if (!knowledgeRepo.existsByIdAndAgentId(knowledgeId, agentId)) {
            throw EntityNotFoundException("Knowledge $knowledgeId not found for agent $agentId")
        }

        // reject files with no name / no extension (safety)
        if (fileName.isBlank()) {
            throw BadRequestException("File name is missing")
        }

        // ------------------------------
        // Chunk the document
        // ------------------------------
        val documents =
            documentChunker.splitDocumentIntoChunks(file)

        if (documents.isEmpty()) {
            throw BadRequestException("File contains no readable text: $fileName")
        }

        // Determine next chunk order (important for multi-file)
        var currentOrder =
            chunkService.getNextChunkOrderForKnowledge(agentId, knowledgeId)

        // ------------------------------
        // Create chunks (DB + vector store)
        // ------------------------------
        documents.forEachIndexed { index, doc ->

            // Enrich metadata with file info for better traceability
            val enrichedMetadata: Map<String, Any> =
                doc.metadata +
                    mapOf(
                        "fileName" to fileName,
                        "fileOrder" to index,
                        "importTimestamp" to System.currentTimeMillis(),
                    )

            chunkService.addChunk(
                agentId = agentId,
                knowledgeId = knowledgeId,
                chunkOrder = currentOrder,
                content = doc.text ?: "",
                metadata = enrichedMetadata,
            )
            currentOrder++
        }

        logger.info(
            "Imported FILE for agent={}, knowledge={}, file={}, segments={}",
            agentId,
            knowledgeId,
            fileName,
            documents.size,
        )

        return KnowledgeImportingResponseVm(
            originalFilename = fileName,
            numberOfSegment = documents.size,
        )
    }
}
