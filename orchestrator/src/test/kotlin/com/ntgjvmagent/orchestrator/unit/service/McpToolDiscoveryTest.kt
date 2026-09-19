package com.ntgjvmagent.orchestrator.unit.service

import com.ntgjvmagent.orchestrator.dto.request.AuthenticationRequestDto
import com.ntgjvmagent.orchestrator.exception.BadRequestException
import com.ntgjvmagent.orchestrator.service.HttpMcpToolDiscovery
import com.ntgjvmagent.orchestrator.service.McpSyncClientFactory
import com.ntgjvmagent.orchestrator.utils.AuthType
import com.ntgjvmagent.orchestrator.utils.McpClientTransportType
import io.mockk.mockk
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class McpToolDiscoveryTest {
    private val discovery = HttpMcpToolDiscovery(mockk<McpSyncClientFactory>())

    @Test
    fun `builds the canonical Streamable HTTP transport and retains legacy SSE`() {
        val noAuth = AuthenticationRequestDto()

        assertIs<HttpClientStreamableHttpTransport>(
            discovery.buildTransport("http://localhost:9003", "/mcp", noAuth, McpClientTransportType.STREAMABLE),
        )
        assertIs<HttpClientSseClientTransport>(
            discovery.buildTransport("http://localhost:9003", "/sse", noAuth, McpClientTransportType.SSE),
        )
        assertFailsWith<BadRequestException> {
            discovery.buildTransport("http://localhost:9003", "/mcp", noAuth, McpClientTransportType.STDIO)
        }
    }

    @Test
    fun `applies supported HTTP authentication without exposing credentials`() {
        val bearer = request(AuthenticationRequestDto(AuthType.BEARER, token = "bearer-secret"))
        val apiKey = request(AuthenticationRequestDto(AuthType.API_KEY, token = "api-secret"))
        val custom =
            request(
                AuthenticationRequestDto(
                    AuthType.CUSTOM_HEADER,
                    token = "custom-secret",
                    headerName = "X-MCP-Token",
                ),
            )
        val none = request(AuthenticationRequestDto())

        assertEquals("Bearer bearer-secret", bearer.headers().firstValue("Authorization").orElseThrow())
        assertEquals("api-secret", apiKey.headers().firstValue("X-API-Key").orElseThrow())
        assertEquals("custom-secret", custom.headers().firstValue("X-MCP-Token").orElseThrow())
        assertNull(none.headers().firstValue("Authorization").orElse(null))
    }

    @Test
    fun `rejects incomplete HTTP authentication configuration`() {
        assertFailsWith<BadRequestException> {
            discovery.buildRequestBuilder(AuthenticationRequestDto(AuthType.BEARER))
        }
        assertFailsWith<BadRequestException> {
            discovery.buildRequestBuilder(
                AuthenticationRequestDto(AuthType.CUSTOM_HEADER, token = "secret"),
            )
        }
    }

    private fun request(authorization: AuthenticationRequestDto) =
        discovery
            .buildRequestBuilder(authorization)
            .uri(URI.create("http://localhost:9003/mcp"))
            .build()
}
