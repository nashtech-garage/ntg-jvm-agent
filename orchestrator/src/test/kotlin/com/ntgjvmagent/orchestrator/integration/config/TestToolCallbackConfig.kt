package com.ntgjvmagent.orchestrator.integration.config

import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TestToolCallbackConfig {
    @Bean
    fun toolCallbackProvider(): ToolCallbackProvider {
        // Provide a dummy implementation — it returns no tools
        return ToolCallbackProvider { arrayOf<ToolCallback>() }
    }
}
