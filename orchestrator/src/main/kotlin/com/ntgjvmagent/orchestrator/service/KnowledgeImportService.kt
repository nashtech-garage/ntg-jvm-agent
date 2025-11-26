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
    private val systemSettingService: SystemSettingService,
) {
    private val logger = LoggerFactory.getLogger(KnowledgeImportService::class.java)

    @Transactional
    fun importDocument(
        agentId: UUID,
        knowledgeId: UUID,
        file: MultipartFile,
    ): KnowledgeImportingResponseVm {
        val fileName = file.originalFilename
        logger.info("Importing document for agent {}: {}, file type: {}", agentId, fileName, file.contentType)

        val fileType = fileName?.split(".")?.last()
        val setting = systemSettingService.getSystemSetting()
        if(setting.allowedFileTypes!!.split(",").contains(fileType)){
            throw BadRequestException("File type is not allowed")
        }

        val maxSize = setting.maximumSizeFileUpload!! * 1024 * 1024
        if(maxSize < file.size){
            throw BadRequestException("File size exceeded")
        }

        // Check that knowledge exists for this agent
        if (!knowledgeRepo.existsByIdAndAgentId(knowledgeId, agentId)) {
            throw EntityNotFoundException("Knowledge $knowledgeId not found for agent $agentId")
        }

        // Split file into chunks
        val documents = documentChunker.splitDocumentIntoChunks(file)
        if (documents.isEmpty()) {
            throw BadRequestException("File is empty or contains no readable text")
        }

        // Determine starting chunk_order for this knowledge
        var currentOrder = chunkService.getNextChunkOrderForKnowledge(agentId, knowledgeId)

        // Create DB chunks and add to vector store
        documents.forEach { doc ->
            chunkService.addChunk(
                agentId = agentId,
                knowledgeId = knowledgeId,
                chunkOrder = currentOrder,
                content = doc.text!!,
                metadata = doc.metadata,
            )
            currentOrder++
        }

        val numberOfSegments = documents.size
        logger.info("Document imported for agent {}: {}, segments: {}", agentId, fileName, numberOfSegments)

        return KnowledgeImportingResponseVm(
            originalFilename = fileName!!,
            numberOfSegment = numberOfSegments,
        )
    }
}
