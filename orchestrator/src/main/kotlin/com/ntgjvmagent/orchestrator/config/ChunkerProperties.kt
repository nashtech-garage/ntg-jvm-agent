package com.ntgjvmagent.orchestrator.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "chunker")
class ChunkerProperties {
    companion object {
        const val DEFAULT_CHUNK_SIZE = 500
        const val DEFAULT_MIN_CHUNK_SIZE_CHARS = 300
        const val DEFAULT_MIN_CHUNK_LENGTH_TO_EMBED = 10
        const val DEFAULT_MAX_NUM_CHUNKS = 1000
    }

    val profiles: MutableMap<String, ChunkerProfile> = mutableMapOf()

    class ChunkerProfile {
        var chunkSize: Int = DEFAULT_CHUNK_SIZE
        var minChunkSizeChars: Int = DEFAULT_MIN_CHUNK_SIZE_CHARS
        var minChunkLengthToEmbed: Int = DEFAULT_MIN_CHUNK_LENGTH_TO_EMBED
        var maxNumChunks: Int = DEFAULT_MAX_NUM_CHUNKS
        var keepSeparator: Boolean = true
    }
}
