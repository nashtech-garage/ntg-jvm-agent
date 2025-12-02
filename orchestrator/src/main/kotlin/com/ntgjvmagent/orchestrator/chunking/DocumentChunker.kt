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
    // Initialize splitters dynamically from application properties
    private val splitters: Map<String, TokenTextSplitter> =
        chunkerProperties.profiles.mapValues { (_, p) ->
            TokenTextSplitter(
                p.chunkSize,
                p.minChunkSizeChars,
                p.minChunkLengthToEmbed,
                p.maxNumChunks,
                p.keepSeparator,
            )
        }

    /**
     * Split a MultipartFile into token-aware chunks.
     *
     * @param file MultipartFile to process
     * @param profileName Optional explicit profile override
     */
    fun splitDocumentIntoChunks(
        file: MultipartFile,
        profileName: String? = null,
    ): List<Document> {
        val fileName = file.originalFilename ?: "unknown"
        val extension = fileName.substringAfterLast('.', "").lowercase()
        val text = textExtractor.extract(file, extension)

        if (text.isBlank() || file.isEmpty) return emptyList()

        val chosenProfile = profileName ?: profileDetector.detect(text, extension)
        val splitter = splitters[chosenProfile] ?: error("Unknown splitter profile: $chosenProfile")

        val originalDoc = Document.builder().text(text).build()
        val splitDocs = splitter.apply(listOf(originalDoc))

        var searchIndex = 0
        val results = mutableListOf<Document>()

        splitDocs.forEach { d ->
            val chunkText = d.text ?: ""
            // Find the occurrence starting from last search index to avoid earlier matches
            val start = text.indexOf(chunkText, searchIndex)
            val resolvedStart =
                if (start >= 0) {
                    start
                } else {
                    // fallback: try to find by trimming whitespace
                    val trimmed = chunkText.trim()
                    if (trimmed.isEmpty()) {
                        searchIndex
                    } else {
                        text.indexOf(trimmed, searchIndex).takeIf { it >= 0 } ?: searchIndex
                    }
                }
            val end = resolvedStart + chunkText.length
            searchIndex = end.coerceAtLeast(searchIndex)

            val metadata = mutableMapOf<String, Any>()
            if (fileName.isNotBlank()) metadata["fileName"] = fileName
            metadata["profile"] = chosenProfile
            metadata["charStart"] = resolvedStart
            metadata["charEnd"] = end

            val docWithMeta =
                Document
                    .builder()
                    .text(chunkText)
                    .metadata(metadata)
                    .build()
            results += docWithMeta
        }

        return results
    }
}
