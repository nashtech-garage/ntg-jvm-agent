package com.ntgjvmagent.orchestrator.unit.component

import com.ntgjvmagent.orchestrator.component.AppListener
import com.ntgjvmagent.orchestrator.config.JacksonConfig
import com.ntgjvmagent.orchestrator.entity.Tool
import com.ntgjvmagent.orchestrator.repository.ToolRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.ai.tool.ToolCallbackProvider
import kotlin.test.Test
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
        verify(exactly = 0) { repository.saveAll(any<List<Tool>>()) }
    }
}
