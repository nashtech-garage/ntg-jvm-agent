package com.ntgjvmagent.orchestrator.integration

import com.ntgjvmagent.orchestrator.integration.config.PostgresTestContainer
import org.junit.jupiter.api.Tag
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration

@SpringBootTest
@ImportAutoConfiguration(
    exclude = [
        org.springframework.ai.mcp.client.autoconfigure.McpClientAutoConfiguration::class,
        org.springframework.ai.mcp.client.autoconfigure.McpToolCallbackAutoConfiguration::class,
        org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration::class,
    ],
)
@ContextConfiguration(initializers = [PostgresTestContainer.Initializer::class])
@Tag("integration")
abstract class BaseIntegrationTest
