package com.ntgjvmagent.orchestrator.component

import com.ntgjvmagent.orchestrator.service.LeasedToolCallbacks
import com.ntgjvmagent.orchestrator.service.ToolService
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.stereotype.Component

@Component
class GlobalToolCallbackProvider(
    private val delegate: ToolCallbackProvider,
    private val toolService: ToolService,
) {
    fun getToolCallbacks(): LeasedToolCallbacks {
        val toolCallbacks = delegate.toolCallbacks.toList()
        val externalTools = toolService.loadExternalToolCallbackFromDb()
        return LeasedToolCallbacks(toolCallbacks + externalTools.callbacks) { externalTools.close() }
    }
}
