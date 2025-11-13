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
        val result: List<Document> =
            if (file.isEmpty) {
                emptyList()
            } else {
                val fileName = file.originalFilename ?: "unknown"
                val extension = fileName.substringAfterLast('.', "").lowercase()
                val text = textExtractor.extract(file, extension)

                if (text.isBlank()) {
                    emptyList()
                } else {
                    val chosenProfile = profileName ?: profileDetector.detect(text, extension)
                    val splitter =
                        splitters[chosenProfile]
                            ?: error("Unknown splitter profile: $chosenProfile")

                    val metadata = mutableMapOf<String, Any>()
                    if (fileName.isNotBlank()) metadata["source"] = fileName
                    metadata["profile"] = chosenProfile

                    val document =
                        Document
                            .builder()
                            .text(text)
                            .metadata(metadata)
                            .build()

                    splitter.apply(listOf(document))
                }
            }

        return result
    }
}
