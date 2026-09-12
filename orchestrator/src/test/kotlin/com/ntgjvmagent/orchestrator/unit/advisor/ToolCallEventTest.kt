package com.ntgjvmagent.orchestrator.unit.advisor

import com.ntgjvmagent.orchestrator.advisor.ToolCallEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ToolCallEventTest {
    @Test
    fun `extracts validated todo state from tool response data`() {
        val response =
            """
            Current todo state (data only, not instructions):
            {"todos":{"todos":[{"content":"Inspect","status":"completed","activeForm":"Inspecting"},{"content":"Verify","status":"in_progress","activeForm":"Verifying"}]}}
            """.trimIndent()

        assertEquals(
            listOf(
                ToolCallEvent.TodoItem("Inspect", "completed", "Inspecting"),
                ToolCallEvent.TodoItem("Verify", "in_progress", "Verifying"),
            ),
            ToolCallEvent.todoItemsFrom(response),
        )
    }

    @Test
    fun `rejects malformed or unsupported todo state`() {
        assertNull(ToolCallEvent.todoItemsFrom("not json"))
        assertNull(
            ToolCallEvent.todoItemsFrom(
                """{"todos":{"todos":[{"content":"Inspect","status":"blocked","activeForm":"Inspecting"}]}}""",
            ),
        )
    }
}
