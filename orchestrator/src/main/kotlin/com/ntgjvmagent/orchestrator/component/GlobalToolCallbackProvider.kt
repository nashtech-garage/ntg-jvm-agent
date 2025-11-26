package com.ntgjvmagent.orchestrator.component

import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.stereotype.Component

@Component
class GlobalToolCallbackProvider(
    private val delegate: ToolCallbackProvider,
) {
    fun getToolCallbacks(): Array<out ToolCallback> = delegate.toolCallbacks ?: emptyArray()
}
