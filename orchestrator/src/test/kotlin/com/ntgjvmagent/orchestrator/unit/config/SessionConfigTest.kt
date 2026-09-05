package com.ntgjvmagent.orchestrator.unit.config

import com.ntgjvmagent.orchestrator.config.SessionConfig
import com.ntgjvmagent.orchestrator.config.ToolCallingConfig
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.ai.session.SessionService

class SessionConfigTest {
    @Test
    fun `compaction remains disabled`() {
        val config = SessionConfig()
        val advisor =
            config.sessionMemoryAdvisor(
                mockk<SessionService>(),
                config.sessionEventIdGenerator(),
            )

        assertEquals(ToolCallingConfig.TOOL_CALLING_ADVISOR_ORDER + 1, advisor.order)
        assertNull(field(advisor, "compactionTrigger"))
        assertNull(field(advisor, "compactionStrategy"))
    }

    private fun field(
        target: Any,
        name: String,
    ): Any? =
        target.javaClass
            .getDeclaredField(name)
            .also { it.isAccessible = true }
            .get(target)
}
