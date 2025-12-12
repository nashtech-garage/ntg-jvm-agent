package com.ntgjvmagent.orchestrator.chunking

import com.ntgjvmagent.orchestrator.config.ChunkerProperties
import org.springframework.ai.document.Document
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
class DocumentChunker(
    chunkerProperties: ChunkerProperties,
    private val textExtractor: DocumentTextExtractor,
    private val profileDetector: ChunkerProfileDetector,
) {
    /**
     * Raw profile configurations loaded from application.properties
     */
    private val profileConfigs = chunkerProperties.profiles

    /**
     * Prebuilt splitters
     */
    private val splitters: Map<String, TokenTextSplitter> =
        profileConfigs.mapValues { (_, cfg) ->
            TokenTextSplitter(
                cfg.chunkSize,
                cfg.minChunkSizeChars,
                cfg.minChunkLengthToEmbed,
                cfg.maxNumChunks,
                cfg.keepSeparator,
            )
        }

    private fun selectProfile(
        text: String,
        extension: String?,
        override: String?,
    ): String {
        val profile = override ?: profileDetector.detect(text, extension)
        return profile
    }

    /** --------------------------------------------------------
     *  CHUNK RAW TEXT (Web URL, API, DB, Sitemap, Inline)
     * ---------------------------------------------------------*/
    fun chunkText(
        text: String,
        metadata: Map<String, Any?> = emptyMap(),
        profileOverride: String? = null,
        extension: String? = null,
    ): List<Document> {
        var result: List<Document> = emptyList()

        val cleaned = normalizeText(text)

        if (cleaned.isBlank()) return result

        val profile = selectProfile(cleaned, extension, profileOverride)
        val splitter = splitters[profile] ?: error("Unknown splitter profile: $profile")

        val rootDoc =
            Document
                .builder()
                .text(cleaned)
                .metadata(metadata + ("profile" to profile))
                .build()

        val rawChunks = splitter.apply(listOf(rootDoc)).toList()

        val finalChunks =
            rawChunks.map { chunk ->
                Document
                    .builder()
                    .id(chunk.id)
                    .text(chunk.text)
                    .metadata(chunk.metadata)
                    .build()
            }

        result = finalChunks

        return result
    }

    /** --------------------------------------------------------
     *  CHUNK FILE (PDF, DOCX, TXT, CSV, etc.)
     * ---------------------------------------------------------*/
    fun chunkFile(
        file: MultipartFile,
        profileOverride: String? = null,
    ): List<Document> {
        var result: List<Document> = emptyList()

        if (file.isEmpty) return result

        val fileName = file.originalFilename ?: "unknown"
        val extension = fileName.substringAfterLast('.', "").lowercase()

        val extracted = textExtractor.extract(file, extension)

        if (extracted.isNotBlank()) {
            val metadata =
                mapOf(
                    "source" to fileName,
                    "extension" to extension,
                    "fileSize" to file.size,
                )

            val chunked =
                chunkText(
                    text = extracted,
                    metadata = metadata,
                    profileOverride = profileOverride,
                    extension = extension,
                )

            result = chunked
        }

        return result
    }

    /** --------------------------------------------------------
     *  HELPERS
     * ---------------------------------------------------------*/
    private fun normalizeText(text: String): String =
        text
            .replace("\r\n", "\n")
            .replace("\n\n+".toRegex(), "\n")
            .trim()
}
