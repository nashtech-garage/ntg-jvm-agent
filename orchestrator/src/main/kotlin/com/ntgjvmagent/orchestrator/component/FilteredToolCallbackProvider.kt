package com.ntgjvmagent.orchestrator.component

import org.springframework.ai.tool.ToolCallback
import org.springframework.stereotype.Component

@Component
class FilteredToolCallbackProvider {
    fun filterCallbacksByToolNames(
        callbacks: Array<out ToolCallback?>,
        allowedTools: List<String>,
    ): List<ToolCallback?> {
        if (allowedTools.isEmpty()) return emptyList()

        return callbacks.filter { cb ->
            val toolName =
                cb
                    ?.toolDefinition
                    ?.name()
                    ?.takeIf { it.isNotBlank() }
                    ?.substringAfterLast("_")
                    ?.takeIf { it.isNotBlank() }

            toolName in allowedTools
        }
    }
}
