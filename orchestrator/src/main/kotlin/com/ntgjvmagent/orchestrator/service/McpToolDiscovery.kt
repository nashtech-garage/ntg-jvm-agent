package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.dto.request.AuthenticationRequestDto
import com.ntgjvmagent.orchestrator.exception.BadRequestException
import com.ntgjvmagent.orchestrator.utils.AuthType
import com.ntgjvmagent.orchestrator.utils.McpClientTransportType
import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport
import io.modelcontextprotocol.spec.McpClientTransport
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider
import org.springframework.ai.tool.ToolCallback
import org.springframework.stereotype.Component
import java.net.http.HttpRequest
import java.time.Duration

interface McpToolDiscovery {
    fun discover(
        baseUrl: String,
        endpoint: String,
        authorization: AuthenticationRequestDto,
        transportType: McpClientTransportType,
    ): List<ToolCallback>
}

@Component
class HttpMcpToolDiscovery : McpToolDiscovery {
    override fun discover(
        baseUrl: String,
        endpoint: String,
        authorization: AuthenticationRequestDto,
        transportType: McpClientTransportType,
    ): List<ToolCallback> {
        val transport = buildTransport(baseUrl, endpoint, authorization, transportType)
        val client =
            McpClient
                .sync(transport)
                .initializationTimeout(Duration.ofSeconds(INITIALIZATION_TIMEOUT_SECONDS))
                .requestTimeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                .build()
        client.initialize()
        return SyncMcpToolCallbackProvider.syncToolCallbacks(mutableListOf<McpSyncClient>(client))
    }

    internal fun buildTransport(
        baseUrl: String,
        endpoint: String,
        authorization: AuthenticationRequestDto,
        transportType: McpClientTransportType,
    ): McpClientTransport {
        val requestBuilder = buildRequestBuilder(authorization)

        return when (transportType) {
            McpClientTransportType.SSE -> {
                HttpClientSseClientTransport
                    .builder(baseUrl)
                    .sseEndpoint(endpoint)
                    .requestBuilder(requestBuilder.header("accept", "text/event-stream"))
                    .build()
            }

            McpClientTransportType.STREAMABLE -> {
                HttpClientStreamableHttpTransport
                    .builder(baseUrl)
                    .endpoint(endpoint)
                    .requestBuilder(requestBuilder.header("accept", "application/json, text/event-stream"))
                    .build()
            }

            McpClientTransportType.STDIO -> {
                throw BadRequestException("STDIO transport is not supported for database-defined MCP tools")
            }
        }
    }

    internal fun buildRequestBuilder(authorization: AuthenticationRequestDto): HttpRequest.Builder {
        val requestBuilder = HttpRequest.newBuilder().header("content-type", "application/json")
        when (authorization.type) {
            AuthType.BEARER -> {
                requestBuilder.header("Authorization", "Bearer ${requiredToken(authorization)}")
            }

            AuthType.API_KEY -> {
                requestBuilder.header("X-API-Key", requiredToken(authorization))
            }

            AuthType.CUSTOM_HEADER -> {
                requestBuilder.header(
                    requiredHeaderName(authorization),
                    requiredToken(authorization),
                )
            }

            else -> {
                Unit
            }
        }
        return requestBuilder
    }

    private fun requiredToken(authorization: AuthenticationRequestDto): String =
        authorization.token?.takeIf { it.isNotBlank() }
            ?: throw BadRequestException("A non-empty token is required for ${authorization.type} authentication")

    private fun requiredHeaderName(authorization: AuthenticationRequestDto): String =
        authorization.headerName?.takeIf { it.isNotBlank() }
            ?: throw BadRequestException("A non-empty header name is required for CUSTOM_HEADER authentication")

    private companion object {
        const val INITIALIZATION_TIMEOUT_SECONDS = 5L
        const val REQUEST_TIMEOUT_SECONDS = 10L
    }
}
