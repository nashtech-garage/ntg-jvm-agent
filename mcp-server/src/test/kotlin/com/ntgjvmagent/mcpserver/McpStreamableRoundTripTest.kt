package com.ntgjvmagent.mcpserver

import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport
import io.modelcontextprotocol.spec.McpSchema
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(McpStreamableRoundTripTest.FixedClockConfig::class)
class McpStreamableRoundTripTest(
    @LocalServerPort private val port: Int,
) {
    @Test
    fun `streamable endpoint discovers and invokes datetime tool`() {
        val transport =
            HttpClientStreamableHttpTransport
                .builder("http://127.0.0.1:$port")
                .endpoint("/mcp")
                .build()

        McpClient
            .sync(transport)
            .initializationTimeout(Duration.ofSeconds(5))
            .requestTimeout(Duration.ofSeconds(5))
            .build()
            .use { client ->
                client.initialize()
                val tools = client.listTools().tools()
                assertEquals(setOf("getCurrentDatetime", "searchOnline"), tools.map { it.name() }.toSet())
                val datetimeTool = tools.single { it.name() == "getCurrentDatetime" }
                assertEquals("Return the current UTC datetime as an ISO-8601 timestamp", datetimeTool.description())
                assertEquals("object", datetimeTool.inputSchema()["type"])
                assertEquals(emptyMap<String, Any>(), datetimeTool.inputSchema()["properties"])
                assertEquals(emptyList<String>(), datetimeTool.inputSchema()["required"])
                assertEquals(false, datetimeTool.inputSchema()["additionalProperties"])

                val result =
                    client.callTool(
                        McpSchema.CallToolRequest
                            .builder("getCurrentDatetime")
                            .arguments(emptyMap())
                            .build(),
                    )

                assertFalse(result.isError() == true)
                val text = result.content().filterIsInstance<McpSchema.TextContent>().joinToString { it.text() }
                assertTrue(text.contains("\"datetimeUtc\":\"2026-08-30T09:15:00Z\""), text)
            }
    }

    @TestConfiguration
    class FixedClockConfig {
        @Bean
        @Primary
        fun fixedClock(): Clock = Clock.fixed(Instant.parse("2026-08-30T09:15:00Z"), ZoneOffset.UTC)
    }
}
