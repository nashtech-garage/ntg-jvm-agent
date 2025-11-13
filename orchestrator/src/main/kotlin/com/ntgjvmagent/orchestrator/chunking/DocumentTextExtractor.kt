package com.ntgjvmagent.orchestrator.chunking

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.nio.charset.StandardCharsets

@Component
class DocumentTextExtractor {
    fun extract(
        file: MultipartFile,
        ext: String,
    ): String =
        when (ext) {
            "pdf" -> extractPdf(file)
            "docx" -> extractDocx(file)
            else -> extractPlainText(file)
        }

    private fun extractPdf(file: MultipartFile): String =
        PDDocument.load(file.inputStream).use { PDFTextStripper().getText(it) }

    private fun extractPlainText(file: MultipartFile): String =
        file.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }

    private fun extractDocx(file: MultipartFile): String =
        "[DOCX text extraction not implemented] - ${file.originalFilename}"
}
