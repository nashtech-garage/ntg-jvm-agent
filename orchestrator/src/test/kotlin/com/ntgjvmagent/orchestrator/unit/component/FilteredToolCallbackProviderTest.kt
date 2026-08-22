package com.ntgjvmagent.orchestrator.unit.component

import com.ntgjvmagent.orchestrator.component.FilteredToolCallbackProvider
import io.mockk.every
import io.mockk.mockk
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.definition.ToolDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FilteredToolCallbackProviderTest {
    private val provider = FilteredToolCallbackProvider()

    @Test
    fun `returns only callbacks assigned to the agent`() {
        val assigned = callbackNamed("assigned")
        val unassigned = callbackNamed("unassigned")

        val result = provider.filterCallbacksByToolNames(listOf(assigned, unassigned), listOf("assigned"))

        assertEquals(listOf(assigned), result)
    }

    @Test
    fun `returns no callbacks when the agent has no assignments`() {
        assertTrue(provider.filterCallbacksByToolNames(listOf(callbackNamed("known")), emptyList()).isEmpty())
    }

    private fun callbackNamed(name: String): ToolCallback {
        val definition = mockk<ToolDefinition>()
        every { definition.name() } returns name
        return mockk {
            every { toolDefinition } returns definition
        }
    }
}
