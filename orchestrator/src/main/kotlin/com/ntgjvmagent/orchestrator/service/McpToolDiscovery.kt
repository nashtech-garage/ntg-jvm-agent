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
        val client = McpClient.sync(transport).build()
        client.initialize()
        return SyncMcpToolCallbackProvider.syncToolCallbacks(mutableListOf<McpSyncClient>(client))
    }

    private fun buildTransport(
        baseUrl: String,
        endpoint: String,
        authorization: AuthenticationRequestDto,
        transportType: McpClientTransportType,
    ): McpClientTransport {
        val requestBuilder = HttpRequest.newBuilder().header("content-type", "application/json")
        when (authorization.type) {
            AuthType.BEARER -> requestBuilder.header("Authorization", "Bearer ${authorization.token}")
            AuthType.API_KEY -> requestBuilder.header("X-API-Key", authorization.token)
            AuthType.CUSTOM_HEADER -> requestBuilder.header(authorization.headerName, authorization.token)
            else -> Unit
        }

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
}
