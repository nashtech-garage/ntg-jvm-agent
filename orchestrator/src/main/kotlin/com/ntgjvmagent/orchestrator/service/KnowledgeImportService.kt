package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.chunking.DocumentChunker
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.KnowledgeFile
import com.ntgjvmagent.orchestrator.exception.BadRequestException
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.repository.KnowledgeFileRepository
import com.ntgjvmagent.orchestrator.viewmodel.KnowledgeImportingResponseVm
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Service
class KnowledgeImportService(
    private val chunkService: KnowledgeChunkService,
    private val knowledgeRepo: AgentKnowledgeRepository,
    private val documentChunker: DocumentChunker,
    private val fileStorageService: KnowledgeFileStorageService,
    private val knowledgeFileRepo: KnowledgeFileRepository,
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

        // Check that knowledge exists for this agent
        if (!knowledgeRepo.existsByIdAndAgentId(knowledgeId, agentId)) {
            throw EntityNotFoundException("Knowledge $knowledgeId not found for agent $agentId")
        }

        // Save original file to storage
        val savedPath = fileStorageService.saveFile(agentId, knowledgeId, file)

        // Persist KnowledgeFile record
        val knowledge =
            knowledgeRepo.findByIdOrNull(knowledgeId)
                ?: throw EntityNotFoundException("Knowledge $knowledgeId not found")
        val fileRecord =
            KnowledgeFile(
                knowledge = knowledge,
                fileName = fileName ?: savedPath.fileName.toString(),
                filePath = savedPath.toAbsolutePath().toString(),
                contentType = file.contentType ?: "application/octet-stream",
                fileSize = file.size,
            )
        val savedFileEntity = knowledgeFileRepo.save(fileRecord)

        // Split file into chunks
        val documents = documentChunker.splitDocumentIntoChunks(file)
        if (documents.isEmpty()) {
            throw BadRequestException("File is empty or contains no readable text")
        }

        // Determine starting chunk_order for this knowledge
        var currentOrder = chunkService.getNextChunkOrderForKnowledge(agentId, knowledgeId)

        // Create DB chunks and add to vector store
        documents.forEach { doc ->
            val docMeta =
                doc.metadata +
                    mapOf(
                        "fileId" to savedFileEntity.id.toString(),
                        "filePath" to savedFileEntity.filePath,
                        "fileName" to savedFileEntity.fileName,
                        "charStart" to (doc.metadata["charStart"] ?: 0),
                        "charEnd" to (doc.metadata["charEnd"] ?: 0),
                    )
            chunkService.addChunk(
                agentId = agentId,
                knowledgeId = knowledgeId,
                chunkOrder = currentOrder,
                content = doc.text!!,
                metadata = docMeta,
            )
            currentOrder++
        }

        val numberOfSegments = documents.size
        logger.info("Document imported for agent {}: {}, segments: {}", agentId, fileName, numberOfSegments)

        return KnowledgeImportingResponseVm(
            originalFilename = fileName!!,
            numberOfSegment = numberOfSegments,
            fileId = savedFileEntity.id!!,
            filePath = savedFileEntity.filePath,
        )
    }
}
