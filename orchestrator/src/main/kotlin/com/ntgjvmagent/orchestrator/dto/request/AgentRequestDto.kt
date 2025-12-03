package com.ntgjvmagent.orchestrator.dto.request

import com.ntgjvmagent.orchestrator.entity.agent.Agent
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class AgentRequestDto(
    @field:NotBlank(message = "Agent name must not be blank")
    @field:Size(max = 100, message = "Agent name must not exceed 100 characters")
    val name: String,
    val description: String? = null,
    val active: Boolean = true,
    val provider: String,
    @field:NotBlank(message = "Base url must not be blank")
    val baseUrl: String,
    @field:NotBlank(message = "Api key must not be blank")
    val apiKey: String,
    @field:NotBlank(message = "Chat completions path must not be blank")
    val chatCompletionsPath: String,
    @field:NotBlank(message = "Model must not be blank")
    @field:Size(max = 100, message = "Model name must not exceed 100 characters")
    val model: String,
    @field:NotBlank(message = "Embedding model must not be blank")
    val embeddingModel: String,
    @field:Min(value = Agent.MIN_DIMENSION.toLong(), message = "Min embedding must be at least {value}")
    val dimension: Int,
    @field:NotBlank(message = "Embeddings path must not be blank")
    val embeddingsPath: String,
    @field:DecimalMin(value = Agent.MIN_TOP_P.toString(), inclusive = true, message = "TopP must be at least {value}")
    @field:DecimalMax(value = Agent.MAX_TOP_P.toString(), inclusive = true, message = "TopP must not exceed {value}")
    val topP: Double = Agent.DEFAULT_TOP_P.toDouble(),
    @field:DecimalMin(
        value = Agent.MIN_TEMPERATURE.toString(),
        inclusive = true,
        message = "Temperature must be at least {value}",
    )
    @field:DecimalMax(
        value = Agent.MAX_TEMPERATURE.toString(),
        inclusive = true,
        message = "Temperature must not exceed {value}",
    )
    val temperature: Double = Agent.DEFAULT_TEMPERATURE.toDouble(),
    @field:Min(value = Agent.MIN_MAX_TOKENS.toLong(), message = "Max tokens must be at least {value}")
    @field:Max(value = Agent.MAX_MAX_TOKENS.toLong(), message = "Max tokens must not exceed {value}")
    val maxTokens: Int = Agent.DEFAULT_MAX_TOKENS,
    @field:DecimalMin(
        value = Agent.MIN_PENALTY.toString(),
        inclusive = true,
        message = "Frequency penalty must be at least {value}",
    )
    @field:DecimalMax(
        value = Agent.MAX_PENALTY.toString(),
        inclusive = true,
        message = "Frequency penalty must not exceed {value}",
    )
    val frequencyPenalty: Double = Agent.DEFAULT_FREQUENCY_PENALTY.toDouble(),
    @field:DecimalMin(
        value = Agent.MIN_PENALTY.toString(),
        inclusive = true,
        message = "Presence penalty must be at least {value}",
    )
    @field:DecimalMax(
        value = Agent.MAX_PENALTY.toString(),
        inclusive = true,
        message = "Presence penalty must not exceed {value}",
    )
    val presencePenalty: Double = Agent.DEFAULT_PRESENCE_PENALTY.toDouble(),
    val settings: Map<String, Any>? = null,
)
