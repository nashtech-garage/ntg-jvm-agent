package com.ntgjvmagent.orchestrator.unit.config

import com.ntgjvmagent.orchestrator.advisor.ToolCallObservingAdvisor
import com.ntgjvmagent.orchestrator.advisor.ToolLoopLoggingAdvisor
import com.ntgjvmagent.orchestrator.config.ToolCallingConfig
import io.micrometer.observation.ObservationRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.core.Ordered
import kotlin.test.assertTrue

class ToolCallingConfigTest {
    @Test
    fun `tool calling advisor is observed and ordered inside retrieval`() {
        val builder = ToolCallingConfig().toolCallingAdvisorBuilder(ObservationRegistry.NOOP)

        assertEquals(Ordered.LOWEST_PRECEDENCE - 100, builder.advisorOrder)
        assertTrue(builder.advisorOrder < ToolCallObservingAdvisor.ORDER)
        assertTrue(ToolCallObservingAdvisor.ORDER < ToolLoopLoggingAdvisor.ORDER)
        assertNotNull(builder.build())
    }
}
