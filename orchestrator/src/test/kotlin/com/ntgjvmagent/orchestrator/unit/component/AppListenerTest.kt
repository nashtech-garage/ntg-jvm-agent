package com.ntgjvmagent.orchestrator.unit.component

import com.ntgjvmagent.orchestrator.component.AppListener
import com.ntgjvmagent.orchestrator.config.JacksonConfig
import com.ntgjvmagent.orchestrator.entity.Tool
import com.ntgjvmagent.orchestrator.repository.ToolRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.definition.ToolDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class AppListenerTest {
    @Test
    fun `empty discovery leaves active persisted tools unchanged`() {
        val activeTool = Tool(name = "getCurrentDatetime").apply { active = true }
        val repository = mockk<ToolRepository>(relaxed = true)
        every { repository.findAll() } returns listOf(activeTool)
        val provider = ToolCallbackProvider { emptyArray() }

        AppListener(provider, repository, JacksonConfig().objectMapper()).onReady()

        assertNull(activeTool.deletedAt)
        verify(exactly = 0) { repository.save(any<Tool>()) }
        verify(exactly = 0) { repository.saveAll(any<List<Tool>>()) }
    }

    @Test
    fun `discovery refreshes persisted metadata without changing activation`() {
        val persistedTool =
            Tool(
                name = "getCurrentDatetime",
                description = "Old description",
                definition = mapOf("type" to "object", "required" to listOf("oldArgument")),
            ).apply { active = false }
        val repository = mockk<ToolRepository>(relaxed = true)
        every { repository.findAll() } returns listOf(persistedTool)
        every { repository.save(any<Tool>()) } answers { firstArg() }
        val definition = mockk<ToolDefinition>()
        every { definition.name() } returns "getCurrentDatetime"
        every { definition.description() } returns "Return the current UTC datetime as an ISO-8601 timestamp"
        every { definition.inputSchema() } returns
            """{"type":"object","properties":{},"required":[],"additionalProperties":false}"""
        val callback = mockk<ToolCallback>()
        every { callback.toolDefinition } returns definition
        val provider = ToolCallbackProvider { arrayOf(callback) }

        AppListener(provider, repository, JacksonConfig().objectMapper()).onReady()

        assertEquals("Return the current UTC datetime as an ISO-8601 timestamp", persistedTool.description)
        assertEquals(emptyList<Any>(), persistedTool.definition?.get("required"))
        assertEquals(false, persistedTool.definition?.get("additionalProperties"))
        assertFalse(persistedTool.active)
        assertNull(persistedTool.deletedAt)
        verify(exactly = 1) { repository.save(persistedTool) }
    }
}
