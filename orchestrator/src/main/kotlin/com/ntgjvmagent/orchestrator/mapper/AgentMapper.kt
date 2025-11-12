package com.ntgjvmagent.orchestrator.mapper

import com.ntgjvmagent.orchestrator.dto.AgentRequestDto
import com.ntgjvmagent.orchestrator.dto.AgentResponseDto
import com.ntgjvmagent.orchestrator.entity.agent.Agent
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
        ).apply { active = request.active }

    fun toResponse(agent: Agent): AgentResponseDto =
        AgentResponseDto(
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
        )

    /**
     * Converts a nullable Double to BigDecimal safely,
     * or falls back to a provided default BigDecimal.
     */
    private fun Double?.toBigDecimalOrDefault(default: BigDecimal): BigDecimal =
        if (this != null) BigDecimal.valueOf(this) else default
}
