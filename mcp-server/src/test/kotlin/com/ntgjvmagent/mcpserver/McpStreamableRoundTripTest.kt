package com.ntgjvmagent.mcpserver

import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport
import io.modelcontextprotocol.spec.McpSchema
import org.junit.jupiter.api.Assertions.assertEquals
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
    fun `streamable endpoint exposes annotated tools and invokes datetime tool`() {
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
                val datetime = tools.single { it.name() == "getCurrentDatetime" }
                assertEquals("Current UTC datetime", datetime.title())
                assertEquals("Return current datetime in UTC format", datetime.description())
                assertEquals(true, datetime.annotations()?.readOnlyHint())
                assertEquals(false, datetime.annotations()?.destructiveHint())
                assertEquals(false, datetime.annotations()?.idempotentHint())
                assertEquals(false, datetime.annotations()?.openWorldHint())

                val search = tools.single { it.name() == "searchOnline" }
                assertEquals("Search the web", search.title())
                assertTrue(search.description().contains("Search the web for up-to-date information."))
                val query = (search.inputSchema()["properties"] as Map<*, *>)["query"] as Map<*, *>
                assertEquals("string", query["type"])
                assertEquals("Search query", query["description"])
                assertTrue((search.inputSchema()["required"] as List<*>).contains("query"))
                assertEquals(true, search.annotations()?.readOnlyHint())
                assertEquals(false, search.annotations()?.destructiveHint())
                assertEquals(false, search.annotations()?.idempotentHint())
                assertEquals(true, search.annotations()?.openWorldHint())

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
