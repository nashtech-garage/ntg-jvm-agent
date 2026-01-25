package com.ntgjvmagent.orchestrator.config

import com.ntgjvmagent.orchestrator.model.ProviderType
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "embedding")
data class EmbeddingProperties(
    val provider: ProviderType,
    val model: String,
    val dimension: Int,
)
