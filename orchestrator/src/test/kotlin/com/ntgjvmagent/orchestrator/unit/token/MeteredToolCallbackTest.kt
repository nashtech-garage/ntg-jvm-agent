package com.ntgjvmagent.orchestrator.unit.token

import com.ntgjvmagent.orchestrator.model.TokenOperation
import com.ntgjvmagent.orchestrator.token.MeteredToolCallback
import com.ntgjvmagent.orchestrator.token.accounting.TokenMeteringService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.definition.ToolDefinition
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class MeteredToolCallbackTest {
    @Test
    fun `delegates tool execution and preserves zero-token accounting event`() {
        val definition = mockk<ToolDefinition>()
        every { definition.name() } returns "clock"
        val delegate = mockk<ToolCallback>()
        every { delegate.toolDefinition } returns definition
        every { delegate.call("{}") } returns "noon"
        val metering = mockk<TokenMeteringService>(relaxed = true)
        val userId = UUID.randomUUID()
        val agentId = UUID.randomUUID()

        val result = MeteredToolCallback(delegate, metering, userId, agentId, "root").call("{}")

        assertEquals("noon", result)
        verify(exactly = 1) {
            metering.record(
                userId = userId,
                agentId = agentId,
                operation = TokenOperation.TOOL,
                usage = match { it.totalTokens == 0 },
                toolName = "clock",
                correlationId = "root:tool:clock",
            )
        }
    }
}
