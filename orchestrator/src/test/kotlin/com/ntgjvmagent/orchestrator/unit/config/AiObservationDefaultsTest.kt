package com.ntgjvmagent.orchestrator.unit.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.Properties

class AiObservationDefaultsTest {
    @Test
    fun `content-bearing AI observations are opt-in`() {
        val properties =
            Properties().apply {
                requireNotNull(
                    AiObservationDefaultsTest::class.java.classLoader
                        .getResourceAsStream("application.properties"),
                ).use { load(it) }
            }

        assertThat(properties.getProperty("spring.ai.tools.observations.include-content"))
            .endsWith(":false}")
        assertThat(properties.getProperty("spring.ai.chat.observations.log-prompt"))
            .endsWith(":false}")
        assertThat(properties.getProperty("spring.ai.chat.observations.log-completion"))
            .endsWith(":false}")
        assertThat(properties.getProperty("spring.ai.chat.observations.include-error-logging"))
            .endsWith(":false}")
    }
}
