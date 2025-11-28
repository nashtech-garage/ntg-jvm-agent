package com.ntgjvmagent.orchestrator.component

import com.ntgjvmagent.orchestrator.utils.Utils
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
            val toolName = Utils.getShortToolName(cb?.toolDefinition?.name())
            toolName in allowedTools
        }
    }
}
