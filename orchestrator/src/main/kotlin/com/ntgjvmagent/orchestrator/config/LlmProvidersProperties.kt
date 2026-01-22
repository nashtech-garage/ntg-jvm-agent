package com.ntgjvmagent.orchestrator.config

import com.ntgjvmagent.orchestrator.model.ProviderType
import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties(prefix = "llm")
@Validated
data class LlmProvidersProperties(
    val providers: Map<ProviderType, ProviderConfig>,
)

@Validated
data class ProviderConfig(
    @field:NotBlank
    val baseUrl: String,
    @field:NotBlank
    val apiKey: String,
    val embeddingsPath: String = "/embeddings",
    val completionsPath: String = "/v1/chat/completions",
)
