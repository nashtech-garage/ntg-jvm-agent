package com.ntgjvmagent.orchestrator.unit.config

import com.ntgjvmagent.orchestrator.advisor.ToolCallObservingAdvisor
import com.ntgjvmagent.orchestrator.advisor.ToolLoopLoggingAdvisor
import com.ntgjvmagent.orchestrator.config.ToolCallingConfig
import com.ntgjvmagent.orchestrator.config.ToolSearchIndexProperties
import com.ntgjvmagent.orchestrator.service.VectorStoreService
import com.ntgjvmagent.orchestrator.tool.SessionScopedVectorToolIndex
import io.micrometer.observation.ObservationRegistry
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor
import org.springframework.ai.tool.toolsearch.index.regex.RegexToolIndex
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.core.Ordered
import kotlin.test.assertTrue

class ToolCallingConfigTest {
    @Test
    fun `tool calling advisor is observed and ordered inside retrieval`() {
        val builder =
            ToolCallingConfig().toolCallingAdvisorBuilder(
                ObservationRegistry.NOOP,
                RegexToolIndex(),
                ToolSearchIndexProperties(),
            )

        assertEquals(Ordered.LOWEST_PRECEDENCE - 100, builder.advisorOrder)
        assertTrue(builder.advisorOrder < ToolCallObservingAdvisor.ORDER)
        assertTrue(ToolCallObservingAdvisor.ORDER < ToolLoopLoggingAdvisor.ORDER)
        assertTrue(builder.build() is ToolSearchToolCallingAdvisor)
    }

    @Test
    fun `production tool index uses the configured vector store`() {
        val vectorStore = mockk<VectorStore>()
        val vectorStoreService = mockk<VectorStoreService>()
        every { vectorStoreService.getVectorStore() } returns vectorStore

        val toolIndex = ToolCallingConfig().toolIndex(vectorStoreService)

        assertTrue(toolIndex is SessionScopedVectorToolIndex)
        assertNotNull(toolIndex)
    }
}
