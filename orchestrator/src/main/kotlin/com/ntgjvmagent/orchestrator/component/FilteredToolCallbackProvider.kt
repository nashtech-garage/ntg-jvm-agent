package com.ntgjvmagent.orchestrator.component

import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.stereotype.Component

@Component
class FilteredToolCallbackProvider(
    private val delegate: ToolCallbackProvider,
) : ToolCallbackProvider {
    override fun getToolCallbacks(): Array<out ToolCallback?> {
        val tools = delegate.toolCallbacks
        print(delegate.toolCallbacks.first().toolDefinition)
        return tools
    }

    fun getCallbacksByToolNames(allowedTools: List<String>): List<ToolCallback?> {
        if (allowedTools.isEmpty()) return emptyList()
        val result =
            getToolCallbacks().filter { cb ->
                cb
                    ?.toolDefinition
                    ?.name()
                    ?.split("_")
                    ?.last() in allowedTools
            }

        return result
    }
}
