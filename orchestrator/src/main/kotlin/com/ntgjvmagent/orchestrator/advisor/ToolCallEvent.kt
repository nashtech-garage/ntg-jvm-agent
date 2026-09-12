package com.ntgjvmagent.orchestrator.advisor

import tools.jackson.databind.json.JsonMapper

/** A tool lifecycle event observed inside the Spring AI tool-calling loop. */
data class ToolCallEvent(
    val id: String,
    val name: String,
    val phase: Phase,
    val todoItems: List<TodoItem>? = null,
) {
    data class TodoItem(
        val content: String,
        val status: String,
        val activeForm: String,
    )

    enum class Phase {
        STARTED,
        COMPLETED,
    }

    companion object {
        private val jsonMapper = JsonMapper.builder().build()
        private val validStatuses = setOf("pending", "in_progress", "completed")

        fun todoItemsFrom(responseData: String): List<TodoItem>? {
            val jsonStart = responseData.indexOf('{')
            if (jsonStart < 0) return null

            return runCatching {
                val root = jsonMapper.readTree(responseData.substring(jsonStart))
                val todos = requireNotNull(root.get("todos"))
                val items = if (todos.isArray) todos else requireNotNull(todos.get("todos"))
                require(items.isArray)

                items.values().map { item ->
                    val content = requireNotNull(item.get("content")?.asString()?.takeIf { it.isNotBlank() })
                    val status = requireNotNull(item.get("status")?.asString()?.takeIf(validStatuses::contains))
                    val activeForm = requireNotNull(item.get("activeForm")?.asString()?.takeIf { it.isNotBlank() })
                    TodoItem(content = content, status = status, activeForm = activeForm)
                }
            }.getOrNull()
        }
    }
}
