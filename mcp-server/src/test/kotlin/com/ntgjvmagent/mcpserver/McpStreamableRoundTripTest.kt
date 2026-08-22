package com.ntgjvmagent.mcpserver

import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport
import io.modelcontextprotocol.spec.McpSchema
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import java.time.Duration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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
                assertTrue(tools.any { it.name() == "getCurrentDatetime" })

                val result =
                    client.callTool(
                        McpSchema.CallToolRequest
                            .builder("getCurrentDatetime")
                            .arguments(emptyMap())
                            .build(),
                    )

                assertFalse(result.isError() == true)
                val text = result.content().filterIsInstance<McpSchema.TextContent>().joinToString { it.text() }
                assertTrue(text.contains(Regex("\\d{4}-\\d{2}-\\d{2}")))
            }
    }
}
