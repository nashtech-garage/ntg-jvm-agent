package com.ntgjvmagent.orchestrator.component

import com.ntgjvmagent.orchestrator.service.ToolService
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.stereotype.Component

@Component
class GlobalToolCallbackProvider(
    private val delegate: ToolCallbackProvider,
    private val toolService: ToolService,
) {
    fun getToolCallbacks(): List<ToolCallback> {
        val toolCallbacks = delegate.toolCallbacks.toList()
        val externalTools = toolService.loadExternalToolCallbackFromDb()
        val list: MutableList<ToolCallback> = mutableListOf()
        list.addAll(toolCallbacks)
        list.addAll(externalTools)
        return list
    }
}
