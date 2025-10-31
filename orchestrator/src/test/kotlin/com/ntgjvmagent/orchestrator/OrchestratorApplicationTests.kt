package com.ntgjvmagent.orchestrator

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.ai.model.tool.ToolCallingManager
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class MockConfig {
    @Bean
    fun toolCallbackProvider(): ToolCallbackProvider = mock(ToolCallbackProvider::class.java)

    @Bean
    fun toolCallingManager(): ToolCallingManager = mock(ToolCallingManager::class.java)
}

@SpringBootTest(
    classes = [MockConfig::class],
    properties = [
        "spring.ai.mcp.client.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration",
    ],
)
class OrchestratorApplicationTests {
    @Test
    fun contextLoads() {
    }
}
