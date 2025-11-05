package com.ntgjvmagent.orchestrator.agent.web.mapper

import com.ntgjvmagent.orchestrator.agent.domain.Agent
import com.ntgjvmagent.orchestrator.agent.web.dto.AgentRequest
import com.ntgjvmagent.orchestrator.agent.web.dto.AgentResponse
import java.math.BigDecimal

object AgentMapper {
    fun toEntity(request: AgentRequest): Agent =
        Agent(
            name = request.name.trim(),
            model = request.model.trim(),
            description = request.description?.trim(),
            temperature = request.temperature.toBigDecimalOrDefault(Agent.DEFAULT_TEMPERATURE),
            maxTokens = request.maxTokens,
            topP = request.topP.toBigDecimalOrDefault(Agent.DEFAULT_TOP_P),
            frequencyPenalty = request.frequencyPenalty.toBigDecimalOrDefault(Agent.DEFAULT_FREQUENCY_PENALTY),
            presencePenalty = request.presencePenalty.toBigDecimalOrDefault(Agent.DEFAULT_PRESENCE_PENALTY),
            active = request.active,
            provider = request.provider,
            settings = request.settings,
        )

    fun toResponse(agent: Agent): AgentResponse =
        AgentResponse(
            id = agent.id!!,
            name = agent.name,
            model = agent.model,
            description = agent.description,
            temperature = agent.temperature.toDouble(),
            maxTokens = agent.maxTokens,
            topP = agent.topP.toDouble(),
            frequencyPenalty = agent.frequencyPenalty.toDouble(),
            presencePenalty = agent.presencePenalty.toDouble(),
            active = agent.active,
            provider = agent.provider,
            settings = agent.settings,
            createdAt = agent.createdAt.toLocalDateTime(),
            updatedAt = agent.updatedAt?.toLocalDateTime(),
        )

    /**
     * Converts a nullable Double to BigDecimal safely,
     * or falls back to a provided default BigDecimal.
     */
    private fun Double?.toBigDecimalOrDefault(default: BigDecimal): BigDecimal =
        if (this != null) BigDecimal.valueOf(this) else default
}
