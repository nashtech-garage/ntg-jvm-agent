package com.ntgjvmagent.orchestrator.component

import com.ntgjvmagent.orchestrator.repository.AgentToolRepository
import com.ntgjvmagent.orchestrator.service.AgentMemoryService
import com.ntgjvmagent.orchestrator.token.MeteredToolCallback
import com.ntgjvmagent.orchestrator.token.accounting.TokenMeteringService
import com.ntgjvmagent.orchestrator.tool.LocalToolCatalog
import org.springframework.ai.tool.ToolCallback
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ToolExecutionFacade(
    private val agentToolRepository: AgentToolRepository,
    private val filteredToolCallbackProvider: FilteredToolCallbackProvider,
    private val globalToolCallbackProvider: GlobalToolCallbackProvider,
    private val localToolCatalog: LocalToolCatalog,
    private val tokenMeteringService: TokenMeteringService,
    private val agentMemoryService: AgentMemoryService? = null,
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

        val assignedCallbacks =
            filteredToolCallbackProvider
                .filterCallbacksByToolNames(allCallbacks, allowedToolNames)
                .filterNotNull()

        val memoryEnabled = agentMemoryService?.isEnabled() == true

        return (localToolCatalog.getToolCallbacks(agentId, memoryEnabled) + assignedCallbacks)
            .distinctBy { it.toolDefinition.name() }
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
