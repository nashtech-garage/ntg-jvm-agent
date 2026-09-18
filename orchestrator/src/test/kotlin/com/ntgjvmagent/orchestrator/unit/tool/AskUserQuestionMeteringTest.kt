package com.ntgjvmagent.orchestrator.unit.tool

import com.ntgjvmagent.orchestrator.component.FilteredToolCallbackProvider
import com.ntgjvmagent.orchestrator.component.GlobalToolCallbackProvider
import com.ntgjvmagent.orchestrator.component.ToolExecutionFacade
import com.ntgjvmagent.orchestrator.repository.AgentToolRepository
import com.ntgjvmagent.orchestrator.service.PendingQuestionService
import com.ntgjvmagent.orchestrator.token.MeteredToolCallback
import com.ntgjvmagent.orchestrator.token.accounting.TokenMeteringService
import com.ntgjvmagent.orchestrator.tool.AskUserQuestionTool
import com.ntgjvmagent.orchestrator.tool.LocalToolCatalog
import com.ntgjvmagent.orchestrator.tool.SupportPolicyTool
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class AskUserQuestionMeteringTest {
    @Test
    fun `question tool is registered through catalog and metered`() {
        val agentToolRepository = mockk<AgentToolRepository>()
        val filteredProvider = mockk<FilteredToolCallbackProvider>()
        val globalProvider = mockk<GlobalToolCallbackProvider>()
        every { agentToolRepository.findByAgentId(any()) } returns emptyList()
        every { globalProvider.getToolCallbacks() } returns emptyList()
        every { filteredProvider.filterCallbacksByToolNames(any(), any()) } returns emptyList()
        val catalog =
            LocalToolCatalog(
                supportPolicyTool = SupportPolicyTool(),
                agentMemoryService = null,
                askUserQuestionTool = AskUserQuestionTool(mockk<PendingQuestionService>()),
            )
        val facade =
            ToolExecutionFacade(
                agentToolRepository = agentToolRepository,
                filteredToolCallbackProvider = filteredProvider,
                globalToolCallbackProvider = globalProvider,
                localToolCatalog = catalog,
                tokenMeteringService = mockk<TokenMeteringService>(relaxed = true),
            )

        val callbacks =
            facade.createToolCallbacks(
                userId = UUID.randomUUID(),
                agentId = UUID.randomUUID(),
                correlationId = "metering-test",
                sessionId = UUID.randomUUID(),
            )
        val question =
            callbacks.single {
                it.toolDefinition.name() == LocalToolCatalog.ASK_USER_QUESTION_TOOL_NAME
            }

        assertTrue(question is MeteredToolCallback)
    }
}
