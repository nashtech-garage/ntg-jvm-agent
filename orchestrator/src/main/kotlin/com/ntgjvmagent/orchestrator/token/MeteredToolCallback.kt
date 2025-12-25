package com.ntgjvmagent.orchestrator.token

import com.ntgjvmagent.orchestrator.model.TokenOperation
import com.ntgjvmagent.orchestrator.token.accounting.TokenMeteringService
import org.springframework.ai.chat.metadata.Usage
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.definition.ToolDefinition
import java.util.UUID

class MeteredToolCallback(
    private val delegate: ToolCallback,
    private val tokenMeteringService: TokenMeteringService,
    private val userId: UUID,
    private val agentId: UUID,
    private val rootCorrelationId: String,
) : ToolCallback {
    override fun getToolDefinition(): ToolDefinition = delegate.toolDefinition

    override fun call(arguments: String): String {
        val toolName = delegate.toolDefinition.name()
        val toolCorrelationId = "$rootCorrelationId:tool:$toolName"

        val result = delegate.call(arguments)

        // Record TOOL invocation (event, not token cost)
        tokenMeteringService.record(
            userId = userId,
            agentId = agentId,
            operation = TokenOperation.TOOL,
            usage = ZeroUsage,
            toolName = toolName,
            correlationId = toolCorrelationId,
        )

        return result
    }

    private object ZeroUsage : Usage {
        override fun getPromptTokens(): Int = 0

        override fun getCompletionTokens(): Int = 0

        override fun getTotalTokens(): Int = 0

        override fun getNativeUsage(): Any? = null
    }
}
