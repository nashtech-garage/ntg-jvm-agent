package com.ntgjvmagent.orchestrator.component

import com.ntgjvmagent.orchestrator.repository.AgentToolRepository
import com.ntgjvmagent.orchestrator.token.TokenMeteringService
import com.ntgjvmagent.orchestrator.token.tool.MeteredToolCallback
import org.springframework.ai.tool.ToolCallback
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ToolExecutionFacade(
    private val agentToolRepository: AgentToolRepository,
    private val filteredToolCallbackProvider: FilteredToolCallbackProvider,
    private val globalToolCallbackProvider: GlobalToolCallbackProvider,
    private val tokenMeteringService: TokenMeteringService,
) {
    fun createToolCallbacks(
        userId: UUID,
        agentId: UUID,
        correlationId: String,
    ): List<ToolCallback> {
        val allowedToolNames =
            agentToolRepository
                .findByAgentId(agentId)
                .map { it.tool.name }

        val allCallbacks = globalToolCallbackProvider.getToolCallbacks()

        return filteredToolCallbackProvider
            .filterCallbacksByToolNames(allCallbacks, allowedToolNames)
            .filterNotNull()
            .map { callback ->
                MeteredToolCallback(
                    delegate = callback,
                    tokenMeteringService = tokenMeteringService,
                    userId = userId,
                    agentId = agentId,
                    rootCorrelationId = correlationId,
                )
            }
    }
}
