package com.ntgjvmagent.orchestrator.chunking

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.stereotype.Component
import java.io.InputStream
import java.nio.charset.StandardCharsets

@Component
class DocumentTextExtractor {
    fun extract(
        input: InputStream,
        ext: String,
    ): String =
        when (ext.lowercase()) {
            "pdf" -> extractPdf(input)
            "docx" -> extractDocx(input)
            else -> extractPlainText(input)
        }

    private fun extractPdf(input: InputStream): String =
        input.use { stream ->
            PDDocument.load(stream).use { doc ->
                PDFTextStripper().getText(doc)
            }
        }

    private fun extractPlainText(input: InputStream): String =
        input.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }

    private fun extractDocx(input: InputStream): String =
        input.use { _ ->
            "[DOCX text extraction not implemented]"
        }
}
