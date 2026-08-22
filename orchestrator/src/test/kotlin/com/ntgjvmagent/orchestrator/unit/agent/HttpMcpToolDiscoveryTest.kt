package com.ntgjvmagent.orchestrator.unit.agent

import com.ntgjvmagent.orchestrator.dto.request.AuthenticationRequestDto
import com.ntgjvmagent.orchestrator.exception.BadRequestException
import com.ntgjvmagent.orchestrator.service.HttpMcpToolDiscovery
import com.ntgjvmagent.orchestrator.service.McpConnection
import com.ntgjvmagent.orchestrator.utils.AuthType
import com.ntgjvmagent.orchestrator.utils.McpClientTransportType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.modelcontextprotocol.client.McpSyncClient
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.ai.tool.ToolCallback
import org.springframework.context.annotation.AnnotationConfigApplicationContext

class HttpMcpToolDiscoveryTest {
    private val callbacks = listOf(mockk<ToolCallback>())
    private val connection = AuthenticationRequestDto()

    @Test
    fun `Spring instantiates discovery with its production constructor`() {
        AnnotationConfigApplicationContext(HttpMcpToolDiscovery::class.java).use { context ->
            assertNotNull(context.getBean(HttpMcpToolDiscovery::class.java))
        }
    }

    @Test
    fun `verification closes its client while discovery reuses and evicts rotated credentials`() {
        val verificationClient = client()
        val cachedClient = client()
        val rotatedClient = client()
        val clients = mutableListOf(verificationClient, cachedClient, rotatedClient)
        val discovery = HttpMcpToolDiscovery({ clients.removeAt(0) }, { callbacks })

        assertSame(
            callbacks,
            discovery.verify("http://localhost:8080", "/mcp", connection, McpClientTransportType.STREAMABLE),
        )
        verify(exactly = 1) { verificationClient.close() }

        val first = discovery.discover("http://localhost:8080", "/mcp", connection, McpClientTransportType.STREAMABLE)
        val second =
            discovery.discover(
                "http://localhost:8080",
                "/mcp",
                AuthenticationRequestDto(),
                McpClientTransportType.STREAMABLE,
            )
        assertSame(first.callbacks, second.callbacks)
        verify(exactly = 1) { cachedClient.initialize() }
        verify(exactly = 0) { cachedClient.close() }

        val rotatedAuth = AuthenticationRequestDto(AuthType.BEARER, "rotated-token")
        val rotated =
            discovery.discover(
                "http://localhost:8080",
                "/mcp",
                rotatedAuth,
                McpClientTransportType.STREAMABLE,
            )
        verify(exactly = 1) { rotatedClient.initialize() }

        discovery.retainActiveConnections(
            setOf(McpConnection("http://localhost:8080", "/mcp", rotatedAuth, McpClientTransportType.STREAMABLE)),
        )
        verify(exactly = 0) { cachedClient.close() }
        verify(exactly = 0) { rotatedClient.close() }

        first.close()
        verify(exactly = 0) { cachedClient.close() }
        second.close()
        verify(exactly = 1) { cachedClient.close() }
        rotated.close()

        discovery.close()
        verify(exactly = 1) { cachedClient.close() }
        verify(exactly = 1) { rotatedClient.close() }
        assertThrows<IllegalStateException> {
            discovery.discover("http://localhost:8080", "/mcp", connection, McpClientTransportType.STREAMABLE)
        }
    }

    @Test
    fun `removed and changed endpoints close unused clients`() {
        val oldClient = client()
        val newClient = client()
        val clients = mutableListOf(oldClient, newClient)
        val discovery = HttpMcpToolDiscovery({ clients.removeAt(0) }, { callbacks })

        val old = discovery.discover("http://localhost:8080", "/old", connection, McpClientTransportType.STREAMABLE)
        val new = discovery.discover("http://localhost:8080", "/new", connection, McpClientTransportType.STREAMABLE)
        discovery.retainActiveConnections(
            setOf(McpConnection("http://localhost:8080", "/new", connection, McpClientTransportType.STREAMABLE)),
        )
        verify(exactly = 0) { oldClient.close() }
        verify(exactly = 0) { newClient.close() }

        discovery.retainActiveConnections(emptySet())
        verify(exactly = 0) { newClient.close() }
        old.close()
        verify(exactly = 1) { oldClient.close() }
        new.close()
        verify(exactly = 1) { newClient.close() }
        discovery.close()
        verify(exactly = 1) { oldClient.close() }
        verify(exactly = 1) { newClient.close() }
    }

    @Test
    fun `failed initialization closes the client and leaves the connection retryable`() {
        val failedClient = client()
        every { failedClient.initialize() } throws IllegalStateException("unavailable")
        val retryClient = client()
        val clients = mutableListOf(failedClient, retryClient)
        val discovery = HttpMcpToolDiscovery({ clients.removeAt(0) }, { callbacks })

        assertThrows<IllegalStateException> {
            discovery.discover("http://localhost:8080", "/mcp", connection, McpClientTransportType.STREAMABLE)
        }
        verify(exactly = 1) { failedClient.close() }

        discovery.discover("http://localhost:8080", "/mcp", connection, McpClientTransportType.STREAMABLE).use {
            assertSame(callbacks, it.callbacks)
        }
        discovery.close()
        verify(exactly = 1) { retryClient.close() }
    }

    @Test
    fun `failed callback discovery closes the initialized client`() {
        val failedClient = client()
        val discovery =
            HttpMcpToolDiscovery({ failedClient }, { throw IllegalStateException("tool listing unavailable") })

        assertThrows<IllegalStateException> {
            discovery.discover("http://localhost:8080", "/mcp", connection, McpClientTransportType.STREAMABLE)
        }
        verify(exactly = 1) { failedClient.close() }
    }

    @Test
    fun `incomplete authentication is rejected before opening a client`() {
        val discovery = HttpMcpToolDiscovery({ error("Client must not be created") }, { callbacks })
        val invalidCredentials =
            listOf(
                AuthenticationRequestDto(AuthType.BEARER),
                AuthenticationRequestDto(AuthType.API_KEY, " "),
                AuthenticationRequestDto(AuthType.CUSTOM_HEADER, "token"),
            )

        invalidCredentials.forEach { authorization ->
            assertThrows<BadRequestException> {
                discovery.verify("http://localhost:8080", "/mcp", authorization, McpClientTransportType.STREAMABLE)
            }
        }
    }

    private fun client(): McpSyncClient =
        mockk {
            every { initialize() } returns mockk()
            every { close() } returns Unit
        }
}
