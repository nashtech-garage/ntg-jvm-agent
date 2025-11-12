package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.exception.BadRequestException
import com.ntgjvmagent.orchestrator.viewmodel.KnowledgeImportingResponseVm
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Service
class KnowledgeImportService(
    private val chunkService: KnowledgeChunkService,
) {
    private val logger = LoggerFactory.getLogger(KnowledgeImportService::class.java)

    fun importDocument(
        knowledgeId: UUID,
        file: MultipartFile,
    ): KnowledgeImportingResponseVm {
        logger.info("Importing document: {}, file type: {}", file.originalFilename, file.contentType)

        val fileName = file.originalFilename ?: file.name
        val fileExtension = fileName.substringAfterLast('.', "").lowercase()
        val contentType = file.contentType

        // Extract text depending on file type
        val text =
            when {
                isTextFile(fileExtension, contentType) -> String(file.bytes, Charsets.UTF_8)
                isPDFFile(fileExtension, contentType) -> extractTextFromPdf(file)
                else -> throw BadRequestException("Only support .txt, .md and .pdf")
            }

        if (text.isBlank()) {
            throw BadRequestException("File is empty or contains no readable text")
        }

        return importText(knowledgeId, fileName, text)
    }

    fun importText(
        knowledgeId: UUID,
        fileName: String,
        text: String,
    ): KnowledgeImportingResponseVm {
        val documents = chunkDocuments(fileName, text)

        // Create DB chunks and add to vector store
        val savedChunks =
            documents.map { doc ->
                chunkService.addChunk(
                    knowledgeId = knowledgeId,
                    content = doc.text!!,
                    metadata = doc.metadata,
                )
            }

        logger.info("Document imported: {}, segments: {}", fileName, documents.size)

        return KnowledgeImportingResponseVm(
            originalFilename = fileName,
            numberOfSegment = savedChunks.size,
        )
    }

    /** ------------------ Chunking Helper ------------------ */
    private fun chunkDocuments(
        fileName: String,
        text: String,
        maxChunkSizeChars: Int = 500,
        minChunkSizeChars: Int = 300,
        minChunkLengthToEmbed: Int = 10,
        maxNumChunks: Int = 1000,
    ): List<Document> {
        val keepSeparator = true
        val document =
            Document
                .builder()
                .text(text)
                .metadata(mapOf("source" to fileName))
                .build()

        val splitter =
            TokenTextSplitter(
                maxChunkSizeChars,
                minChunkSizeChars,
                minChunkLengthToEmbed,
                maxNumChunks,
                keepSeparator,
            )
        return splitter.apply(listOf(document))
    }

    /** ------------------ File Type Helpers ------------------ */
    private fun isTextFile(
        fileExtension: String,
        contentType: String?,
    ) = fileExtension in listOf("txt", "md") || contentType?.startsWith("text/") == true

    private fun isPDFFile(
        fileExtension: String,
        contentType: String?,
    ) = fileExtension == "pdf" && contentType?.equals("application/pdf", ignoreCase = true) == true

    private fun extractTextFromPdf(file: MultipartFile): String {
        PDDocument.load(file.inputStream).use { document ->
            val stripper = PDFTextStripper()
            return stripper.getText(document)
        }
    }
}
