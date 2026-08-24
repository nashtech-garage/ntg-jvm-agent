package com.ntgjvmagent.orchestrator.tool

import org.springframework.ai.support.ToolCallbacks
import org.springframework.ai.tool.ToolCallback
import org.springframework.stereotype.Component

@Component
class LocalToolCatalog(
    supportPolicyTool: SupportPolicyTool,
) {
    private val callbacks = ToolCallbacks.from(supportPolicyTool).toList()

    fun getToolCallbacks(): List<ToolCallback> = callbacks
}
