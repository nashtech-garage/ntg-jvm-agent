package com.ntgjvmagent.orchestrator.config

import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FlywayPlaceholderSyncConfig(
    private val properties: VectorEmbeddingProperties,
) {
    @Bean
    fun flywayCustomizer(): FlywayConfigurationCustomizer =
        FlywayConfigurationCustomizer { config ->
            config.placeholders(
                mapOf(
                    // This key matches the placeholder you use in Flyway SQL scripts
                    "spring.ai.vectorstore.pgvector.embedding-dimension" to properties.embeddingDimension.toString(),
                ),
            )
        }
}
