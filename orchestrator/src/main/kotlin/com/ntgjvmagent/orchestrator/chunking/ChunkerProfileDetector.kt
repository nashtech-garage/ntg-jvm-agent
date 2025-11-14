package com.ntgjvmagent.orchestrator.chunking

import org.springframework.stereotype.Component

@Component
class ChunkerProfileDetector {
    companion object {
        const val TIGHT_THRESHOLD = 2_000
        const val DEFAULT_THRESHOLD = 10_000
        const val CSV_SAMPLE_LINES = 5
    }

    private val extensionToProfile =
        mapOf(
            "txt" to "markdown",
            "md" to "markdown",
            "pdf" to "loose",
            "docx" to "loose",
            "json" to "tight",
            "csv" to "tight",
            "xml" to "tight",
            "java" to "code",
            "kt" to "code",
            "js" to "code",
            "ts" to "code",
            "py" to "code",
            "cpp" to "code",
            "c" to "code",
            "go" to "code",
        )

    fun detect(
        text: String,
        ext: String?,
    ): String {
        ext?.lowercase()?.let { extension ->
            return extensionToProfile[extension] ?: "default"
        }
        return detectFromText(text)
    }

    fun detectFromText(text: String): String {
        val trimmed = text.trimStart()
        return when {
            looksLikeJson(trimmed) -> "tight"
            looksLikeXml(trimmed) -> "tight"
            looksLikeCsv(trimmed) -> "tight"
            looksLikeMarkdown(trimmed) -> "markdown"
            looksLikeCode(trimmed) -> "code"
            text.length < TIGHT_THRESHOLD -> "tight"
            text.length < DEFAULT_THRESHOLD -> "default"
            else -> "loose"
        }
    }

    private fun looksLikeJson(text: String) =
        (text.startsWith("{") && text.endsWith("}")) || (text.startsWith("[") && text.endsWith("]"))

    private fun looksLikeXml(text: String) = text.startsWith("<") && text.endsWith(">") && text.contains("</")

    private fun looksLikeCsv(text: String) =
        text.contains(",") && text.lines().take(CSV_SAMPLE_LINES).all { it.contains(",") }

    private fun looksLikeMarkdown(text: String) =
        text.contains("# ") || text.contains("```") || text.contains("* ") || text.contains("- ")

    private fun looksLikeCode(text: String) =
        text.contains("class ") ||
            text.contains("def ") ||
            text.contains("fun ") ||
            text.contains("public ") ||
            text.contains("private ") ||
            (text.contains("{") && text.contains("}"))
}
