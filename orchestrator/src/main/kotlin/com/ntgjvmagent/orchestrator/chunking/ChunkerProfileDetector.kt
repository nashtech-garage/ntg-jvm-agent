package com.ntgjvmagent.orchestrator.chunking

import org.springframework.stereotype.Component

@Component
class ChunkerProfileDetector {
    companion object {
        const val SMALL_TEXT_THRESHOLD = 2_000
        const val MEDIUM_TEXT_THRESHOLD = 10_000
        const val CSV_SAMPLE_LINES = 5
    }

    // Map file extensions → splitter profiles
    private val extensionToProfile: Map<String, String> =
        mapOf(
            // Markdown-like formats
            "txt" to "markdown",
            "md" to "markdown",
            // Code files
            "java" to "code",
            "kt" to "code",
            "js" to "code",
            "ts" to "code",
            "py" to "code",
            "cpp" to "code",
            "c" to "code",
            "go" to "code",
            // Data formats → semantic (meaning-based)
            "json" to "semantic",
            "csv" to "semantic",
            "xml" to "semantic",
            // PDFs & DOCX often require larger chunking → semantic
            "pdf" to "semantic",
            "docx" to "semantic",
        )

    /**
     * Detect profile based on extension first, fallback to text analysis.
     */
    fun detect(
        text: String,
        ext: String?,
    ): String = extensionToProfile[ext?.lowercase()] ?: detectFromText(text)

    /**
     * Detect profile based on textual patterns.
     */
    fun detectFromText(text: String): String {
        val trimmed = text.trimStart()

        return when {
            looksLikeMarkdown(trimmed) -> "markdown"
            looksLikeCode(trimmed) -> "code"
            looksLikeCsv(trimmed) -> "semantic"
            looksLikeJson(trimmed) -> "semantic"
            looksLikeXml(trimmed) -> "semantic"

            // Very short text → treat as semantic (safe)
            trimmed.length < SMALL_TEXT_THRESHOLD -> "semantic"

            // Medium text → sentence-based splitting is usually ideal
            trimmed.length < MEDIUM_TEXT_THRESHOLD -> "sentence"

            // Large documents (PDFs, reports) → semantic splitting
            else -> "semantic"
        }
    }

    // ---- Helpers -------------------------------------------------------------

    private fun looksLikeJson(text: String) =
        (text.startsWith("{") && text.endsWith("}")) ||
            (text.startsWith("[") && text.endsWith("]"))

    private fun looksLikeXml(text: String) = text.startsWith("<") && text.contains("</")

    private fun looksLikeCsv(text: String) =
        text.contains(",") &&
            text.lines().take(CSV_SAMPLE_LINES).all { it.contains(",") }

    private fun looksLikeMarkdown(text: String) =
        text.contains("# ") ||
            text.contains("```") ||
            text.contains("* ") ||
            text.contains("- ")

    private fun looksLikeCode(text: String) =
        text.contains("class ") ||
            text.contains("def ") ||
            text.contains("fun ") ||
            text.contains("public ") ||
            text.contains("private ") ||
            (text.contains("{") && text.contains("}"))
}
