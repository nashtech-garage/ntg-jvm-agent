package com.ntgjvmagent.orchestrator.chunking

import com.ntgjvmagent.orchestrator.config.ChunkerProperties
import org.springframework.ai.document.Document
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.stereotype.Component
import java.io.InputStream

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
            TokenTextSplitter
                .builder()
                .withChunkSize(cfg.chunkSize)
                .withMinChunkSizeChars(cfg.minChunkSizeChars)
                .withMinChunkLengthToEmbed(cfg.minChunkLengthToEmbed)
                .withMaxNumChunks(cfg.maxNumChunks)
                .withKeepSeparator(cfg.keepSeparator)
                .build()
        }

    private fun selectProfile(
        text: String,
        extension: String?,
        override: String?,
    ): String = override ?: profileDetector.detect(text, extension)

    /* --------------------------------------------------------
     *  CHUNK RAW TEXT (Web URL, API, DB, Sitemap, Inline)
     * ---------------------------------------------------------*/
    fun chunkText(
        text: String,
        metadata: Map<String, Any?> = emptyMap(),
        profileOverride: String? = null,
        extension: String? = null,
    ): List<Document> {
        val cleaned = normalizeText(text)
        if (cleaned.isBlank()) return emptyList()

        val profile = selectProfile(cleaned, extension, profileOverride)
        val splitter =
            splitters[profile]
                ?: error("Unknown splitter profile: $profile")

        val root =
            Document
                .builder()
                .text(cleaned)
                .metadata(metadata + ("profile" to profile))
                .build()

        return splitter.apply(listOf(root)).map { chunk ->
            Document
                .builder()
                .id(chunk.id)
                .text(chunk.text)
                .metadata(chunk.metadata)
                .build()
        }
    }

    /* --------------------------------------------------------
     *  CHUNK FILE (storage-based)
     * ---------------------------------------------------------*/
    fun chunk(
        input: InputStream,
        fileName: String?,
        fileSize: Long? = null,
        profileOverride: String? = null,
    ): List<Document> {
        val safeName = fileName ?: "unknown"
        val extension = safeName.substringAfterLast('.', "").lowercase()

        val extracted =
            textExtractor.extract(input, extension)

        if (extracted.isBlank()) return emptyList()

        val metadata =
            mutableMapOf<String, Any?>(
                "source" to safeName,
                "extension" to extension,
            ).apply {
                if (fileSize != null) put("fileSize", fileSize)
            }

        return chunkText(
            text = extracted,
            metadata = metadata,
            profileOverride = profileOverride,
            extension = extension,
        )
    }

    /* --------------------------------------------------------
     *  HELPERS
     * ---------------------------------------------------------*/
    private fun normalizeText(text: String): String =
        text
            .replace("\r\n", "\n")
            .replace("\n\n+".toRegex(), "\n")
            .trim()
}
