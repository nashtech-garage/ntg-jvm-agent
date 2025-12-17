package com.ntgjvmagent.orchestrator.component

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.ntgjvmagent.orchestrator.dto.internal.ToolDataDto
import com.ntgjvmagent.orchestrator.mapper.ToolMapper
import com.ntgjvmagent.orchestrator.repository.ToolRepository
import com.ntgjvmagent.orchestrator.utils.Constant
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class AppListener(
    private val toolCallbackProvider: ToolCallbackProvider,
    private val toolRepo: ToolRepository,
    private val objectMapper: ObjectMapper,
) {
    @EventListener(ApplicationReadyEvent::class)
    fun onReady() {
        initializeTool()
    }

    private fun initializeTool() {
        val allToolEntities = toolRepo.findAll()
        val toolCallbacks = toolCallbackProvider.toolCallbacks
        if (toolCallbacks.isEmpty() && allToolEntities.isEmpty()) {
            return
        }

        val activeTools = allToolEntities.filter { tool -> tool.active }

        // Handle case add new tools if not exist
        if (toolCallbacks.isNotEmpty()) {
            toolCallbacks.forEach { callback ->
                val toolDefinition = callback.toolDefinition
                val toolName = toolDefinition.name()
                val tool = allToolEntities.find { it.name == toolName }
                if (tool == null) {
                    val definition =
                        objectMapper
                            .readValue(
                                toolDefinition.inputSchema(),
                                object : TypeReference<Map<String, Any>>() {},
                            )
                    val toolEntity =
                        ToolMapper
                            .toEntity(
                                ToolDataDto(
                                    toolName,
                                    Constant.MCP_TOOL_TYPE,
                                    null,
                                    toolDefinition.description(),
                                    definition,
                                ),
                            )
                    toolRepo.save(toolEntity)
                }
            }
        }

        // Handle case don't have any tools are registered in MCP => Inactive all tool in DB
        if (toolCallbacks.isEmpty() && activeTools.isNotEmpty()) {
            val softDeletedTools =
                activeTools.map {
                    it.markDeleted()
                    it
                }
            toolRepo.saveAll(softDeletedTools)
        }
    }
}
