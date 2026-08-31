package com.ntgjvmagent.orchestrator.unit.config

import com.ntgjvmagent.orchestrator.config.ChatReasoningProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ChatReasoningPropertiesTest {
    @Test
    fun `reasoning exposure is disabled by default`() {
        assertThat(ChatReasoningProperties().enabled).isFalse()
    }
}
