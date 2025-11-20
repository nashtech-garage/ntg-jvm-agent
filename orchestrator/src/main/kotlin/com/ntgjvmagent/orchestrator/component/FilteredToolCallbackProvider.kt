package com.ntgjvmagent.orchestrator.component

import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.stereotype.Component

@Component
class FilteredToolCallbackProvider(
    private val delegate: ToolCallbackProvider,
) : ToolCallbackProvider {
    override fun getToolCallbacks(): Array<out ToolCallback?> = delegate.toolCallbacks ?: emptyArray()

    fun getCallbacksByToolNames(allowedTools: List<String>): List<ToolCallback?> {
        if (allowedTools.isEmpty()) return emptyList()
        return getToolCallbacks().filter { cb ->
            val toolName =
                cb
                    ?.toolDefinition
                    ?.name()
                    ?.takeIf { it.isNotBlank() }
                    ?.split("_")
                    ?.lastOrNull()
                    ?.takeIf { it.isNotBlank() }
            toolName in allowedTools
        }
    }
}
