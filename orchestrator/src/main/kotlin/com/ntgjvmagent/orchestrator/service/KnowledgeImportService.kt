package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.chunking.DocumentChunker
import com.ntgjvmagent.orchestrator.exception.BadRequestException
import com.ntgjvmagent.orchestrator.viewmodel.KnowledgeImportingResponseVm
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Service
class KnowledgeImportService(
    private val chunkService: KnowledgeChunkService,
    private val documentChunker: DocumentChunker,
) {
    private val logger = LoggerFactory.getLogger(KnowledgeImportService::class.java)

    fun importDocument(
        knowledgeId: UUID,
        file: MultipartFile,
    ): KnowledgeImportingResponseVm {
        val fileName = file.originalFilename
        logger.info("Importing document: {}, file type: {}", fileName, file.contentType)

        // Split file into chunks
        val documents = documentChunker.splitDocumentIntoChunks(file)
        if (documents.isEmpty()) {
            throw BadRequestException("File is empty or contains no readable text")
        }

        // Determine starting chunk_order for this knowledge
        var currentOrder = chunkService.getNextChunkOrderForKnowledge(knowledgeId)

        // Create DB chunks and add to vector store
        documents.forEach { doc ->
            chunkService.addChunk(
                knowledgeId = knowledgeId,
                chunkOrder = currentOrder,
                content = doc.text!!,
                metadata = doc.metadata,
            )
            currentOrder++
        }

        val numberOfSegment = documents.size
        logger.info("Document imported: {}, segments: {}", fileName, numberOfSegment)

        return KnowledgeImportingResponseVm(
            originalFilename = fileName!!,
            numberOfSegment = numberOfSegment,
        )
    }
}
