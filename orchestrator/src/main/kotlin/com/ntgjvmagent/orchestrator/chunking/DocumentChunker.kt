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
     * Default prebuilt splitters
     */
    private val defaultSplitters: Map<String, TokenTextSplitter> =
        profileConfigs.mapValues { (_, cfg) ->
            TokenTextSplitter(
                cfg.chunkSize,
                cfg.minChunkSizeChars,
                cfg.minChunkLengthToEmbed,
                cfg.maxNumChunks,
                cfg.keepSeparator,
            )
        }

    /**
     * Split file into token-aware chunks with optional runtime overrides.
     * @param file            MultipartFile to process
     * @param profileOverride Explicit profile name (optional)
     */
    fun splitDocumentIntoChunks(
        file: MultipartFile,
        profileOverride: String? = null,
    ): List<Document> =
        run {
            if (file.isEmpty) return@run emptyList()

            val fileName = file.originalFilename ?: "unknown"
            val extension = fileName.substringAfterLast('.', "").lowercase()

            val text = textExtractor.extract(file, extension)
            if (text.isBlank()) return@run emptyList()

            val chosenProfile = profileOverride ?: profileDetector.detect(text, extension)
            val splitter =
                defaultSplitters[chosenProfile]
                    ?: error("Unknown splitter profile: $chosenProfile")

            val metadata =
                mutableMapOf<String, Any>(
                    "source" to fileName,
                    "extension" to extension,
                    "fileSize" to file.size,
                    "profile" to chosenProfile,
                )

            val document =
                Document
                    .builder()
                    .text(text)
                    .metadata(metadata)
                    .build()

            splitter.apply(listOf(document))
        }
}
