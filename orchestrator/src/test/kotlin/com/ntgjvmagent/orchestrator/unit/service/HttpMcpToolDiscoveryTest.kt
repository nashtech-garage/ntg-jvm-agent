package com.ntgjvmagent.orchestrator.unit.service

import com.ntgjvmagent.orchestrator.dto.request.AuthenticationRequestDto
import com.ntgjvmagent.orchestrator.service.HttpMcpToolDiscovery
import com.ntgjvmagent.orchestrator.service.McpSyncClientFactory
import com.ntgjvmagent.orchestrator.utils.McpClientTransportType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.spec.McpSchema
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class HttpMcpToolDiscoveryTest {
    @Test
    fun `reuses initialized client and closes it at shutdown`() {
        val client = mockClient()
        val factory = mockk<McpSyncClientFactory>()
        every { factory.create(any()) } returns client
        val discovery = HttpMcpToolDiscovery(factory)

        repeat(2) {
            discovery.discover(
                "http://localhost:9003",
                "/mcp",
                AuthenticationRequestDto(),
                McpClientTransportType.STREAMABLE,
            )
        }
        discovery.close()

        verify(exactly = 1) { factory.create(any()) }
        verify(exactly = 1) { client.initialize() }
        verify(exactly = 2) { client.listTools() }
        verify(exactly = 1) { client.close() }
    }

    @Test
    fun `closes failed client and does not cache it`() {
        val client = mockk<McpSyncClient>()
        every { client.initialize() } throws IllegalStateException("unavailable")
        every { client.close() } returns Unit
        val factory = mockk<McpSyncClientFactory>()
        every { factory.create(any()) } returns client
        val discovery = HttpMcpToolDiscovery(factory)

        repeat(2) {
            assertFailsWith<IllegalStateException> {
                discovery.discover(
                    "http://localhost:9003",
                    "/mcp",
                    AuthenticationRequestDto(),
                    McpClientTransportType.STREAMABLE,
                )
            }
        }

        verify(exactly = 2) { factory.create(any()) }
        verify(exactly = 2) { client.initialize() }
        verify(exactly = 2) { client.close() }
    }

    @Test
    fun `evicts client when tool discovery fails`() {
        val failedClient = mockk<McpSyncClient>()
        every { failedClient.initialize() } returns mockk()
        every { failedClient.listTools() } throws IllegalStateException("connection lost")
        every { failedClient.close() } returns Unit
        val healthyClient = mockClient()
        val factory = mockk<McpSyncClientFactory>()
        every { factory.create(any()) } returnsMany listOf(failedClient, healthyClient)
        val discovery = HttpMcpToolDiscovery(factory)

        assertFailsWith<IllegalStateException> {
            discovery.discover(
                "http://localhost:9003",
                "/mcp",
                AuthenticationRequestDto(),
                McpClientTransportType.STREAMABLE,
            )
        }
        discovery.discover(
            "http://localhost:9003",
            "/mcp",
            AuthenticationRequestDto(),
            McpClientTransportType.STREAMABLE,
        )

        verify(exactly = 2) { factory.create(any()) }
        verify(exactly = 1) { failedClient.close() }
        verify(exactly = 1) { healthyClient.initialize() }
    }

    private fun mockClient(): McpSyncClient =
        mockk<McpSyncClient>().also { client ->
            every { client.initialize() } returns mockk()
            every { client.listTools() } returns McpSchema.ListToolsResult(emptyList(), null)
            every { client.close() } returns Unit
        }
}
