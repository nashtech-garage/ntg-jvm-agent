package com.ntgjvmagent.orchestrator.mapper

import com.ntgjvmagent.orchestrator.dto.request.AgentRequestDto
import com.ntgjvmagent.orchestrator.dto.response.AgentListResponseDto
import com.ntgjvmagent.orchestrator.dto.response.AgentResponseDto
import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.utils.toRelativeString
import java.math.BigDecimal

object AgentMapper {
    fun toEntity(request: AgentRequestDto): Agent =
        Agent(
            name = request.name,
            model = request.model,
            description = request.description,
            temperature = request.temperature.toBigDecimalOrDefault(Agent.DEFAULT_TEMPERATURE),
            maxTokens = request.maxTokens,
            topP = request.topP.toBigDecimalOrDefault(Agent.DEFAULT_TOP_P),
            frequencyPenalty = request.frequencyPenalty.toBigDecimalOrDefault(Agent.DEFAULT_FREQUENCY_PENALTY),
            presencePenalty = request.presencePenalty.toBigDecimalOrDefault(Agent.DEFAULT_PRESENCE_PENALTY),
            provider = request.provider,
            settings = request.settings,
            baseUrl = request.baseUrl,
            apiKey = request.apiKey,
            chatCompletionsPath = request.chatCompletionsPath,
            embeddingsPath = request.embeddingsPath,
            embeddingModel = request.embeddingModel,
            dimension = request.dimension,
        ).apply { active = request.active }

    fun toResponse(agent: Agent): AgentResponseDto =
        AgentResponseDto(
            id = agent.id!!,
            name = agent.name,
            description = agent.description,
            active = agent.active,
            provider = agent.provider,
            baseUrl = agent.baseUrl,
            apiKey = agent.apiKey,
            chatCompletionsPath = agent.chatCompletionsPath,
            model = agent.model,
            embeddingModel = agent.embeddingModel,
            dimension = agent.dimension,
            embeddingsPath = agent.embeddingsPath,
            topP = agent.topP.toDouble(),
            temperature = agent.temperature.toDouble(),
            maxTokens = agent.maxTokens,
            frequencyPenalty = agent.frequencyPenalty.toDouble(),
            presencePenalty = agent.presencePenalty.toDouble(),
            settings = agent.settings,
        )

    fun toListResponse(agent: Agent): AgentListResponseDto =
        AgentListResponseDto(
            id = agent.id!!,
            name = agent.name,
            model = agent.model,
            lastModifiedBy = agent.updatedBy?.name ?: "Unknown",
            lastModifiedWhen = agent.updatedAt?.toRelativeString(),
            lastPublishedWhen = "",
            owner = agent.createdBy?.name ?: "Unknown",
            status = if (agent.active) "Active" else "Inactive",
        )

    /**
     * Converts a nullable Double to BigDecimal safely,
     * or falls back to a provided default BigDecimal.
     */
    private fun Double?.toBigDecimalOrDefault(default: BigDecimal): BigDecimal =
        if (this != null) BigDecimal.valueOf(this) else default
}
