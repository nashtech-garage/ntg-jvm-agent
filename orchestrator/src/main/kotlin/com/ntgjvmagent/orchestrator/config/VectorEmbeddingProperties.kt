package com.ntgjvmagent.orchestrator.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "spring.ai.vectorstore.pgvector")
class VectorEmbeddingProperties {
    companion object {
        private const val DEFAULT_EMBEDDING_DIMENSION = 1536
    }

    var embeddingDimension: Int = DEFAULT_EMBEDDING_DIMENSION
}
