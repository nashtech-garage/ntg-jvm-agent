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
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider
import org.springframework.ai.tool.ToolCallback
import org.springframework.stereotype.Component
import java.net.http.HttpRequest
import java.util.concurrent.atomic.AtomicBoolean

interface McpToolDiscovery {
    fun verify(
        baseUrl: String,
        endpoint: String,
        authorization: AuthenticationRequestDto,
        transportType: McpClientTransportType,
    ): List<ToolCallback>

    fun discover(
        baseUrl: String,
        endpoint: String,
        authorization: AuthenticationRequestDto,
        transportType: McpClientTransportType,
    ): LeasedToolCallbacks

    fun retainActiveConnections(connections: Set<McpConnection>)
}

class LeasedToolCallbacks(
    val callbacks: List<ToolCallback>,
    private val release: () -> Unit,
) : AutoCloseable {
    private val closed = AtomicBoolean()

    override fun close() {
        if (closed.compareAndSet(false, true)) release()
    }
}

data class McpConnection(
    val baseUrl: String,
    val endpoint: String,
    val authType: AuthType,
    val token: String?,
    val headerName: String?,
    val transportType: McpClientTransportType,
) {
    constructor(
        baseUrl: String,
        endpoint: String,
        authorization: AuthenticationRequestDto,
        transportType: McpClientTransportType,
    ) : this(baseUrl, endpoint, authorization.type, authorization.token, authorization.headerName, transportType)

    fun authorization() = AuthenticationRequestDto(authType, token, headerName)

    override fun toString(): String =
        "McpConnection(baseUrl=$baseUrl, endpoint=$endpoint, authType=$authType, transportType=$transportType)"
}

@Component
class HttpMcpToolDiscovery internal constructor(
    private val clientFactory: (McpClientTransport) -> McpSyncClient,
    private val callbackFactory: (McpSyncClient) -> List<ToolCallback>,
) : McpToolDiscovery {
    constructor() : this(
        { transport -> McpClient.sync(transport).build() },
        { client -> SyncMcpToolCallbackProvider.syncToolCallbacks(mutableListOf(client)).toList() },
    )

    private val clients = mutableMapOf<McpConnection, ClientCallbacks>()
    private val retiredClients = mutableSetOf<ClientCallbacks>()
    private var closed = false
    private val logger = LoggerFactory.getLogger(HttpMcpToolDiscovery::class.java)

    override fun verify(
        baseUrl: String,
        endpoint: String,
        authorization: AuthenticationRequestDto,
        transportType: McpClientTransportType,
    ): List<ToolCallback> {
        val entry = openClient(baseUrl, endpoint, authorization, transportType)
        return try {
            entry.callbacks
        } finally {
            closeClient(entry)
        }
    }

    @Synchronized
    override fun discover(
        baseUrl: String,
        endpoint: String,
        authorization: AuthenticationRequestDto,
        transportType: McpClientTransportType,
    ): LeasedToolCallbacks {
        check(!closed) { "MCP tool discovery is closed" }
        val key = McpConnection(baseUrl, endpoint, authorization, transportType)
        val entry =
            clients
                .getOrPut(key) {
                    openClient(baseUrl, endpoint, authorization, transportType)
                }
        entry.leases++
        return LeasedToolCallbacks(entry.callbacks) { release(entry) }
    }

    @Synchronized
    override fun retainActiveConnections(connections: Set<McpConnection>) {
        check(!closed) { "MCP tool discovery is closed" }
        val iterator = clients.iterator()
        while (iterator.hasNext()) {
            val (key, client) = iterator.next()
            if (key !in connections) {
                iterator.remove()
                if (client.leases == 0) {
                    closeClient(client)
                } else {
                    retiredClients.add(client)
                }
            }
        }
    }

    @Synchronized
    private fun release(client: ClientCallbacks) {
        client.leases--
        if (client.leases == 0 && retiredClients.remove(client)) closeClient(client)
    }

    private fun closeClient(client: ClientCallbacks) {
        if (client.closed) return
        client.closed = true
        runCatching { client.client.close() }
            .onFailure { logger.warn("Failed to close MCP client", it) }
    }

    @PreDestroy
    @Synchronized
    fun close() {
        closed = true
        (clients.values + retiredClients).forEach(::closeClient)
        clients.clear()
        retiredClients.clear()
    }

    private fun openClient(
        baseUrl: String,
        endpoint: String,
        authorization: AuthenticationRequestDto,
        transportType: McpClientTransportType,
    ): ClientCallbacks {
        val client = clientFactory(buildTransport(baseUrl, endpoint, authorization, transportType))
        var completed = false
        try {
            client.initialize()
            val result = ClientCallbacks(client, callbackFactory(client))
            completed = true
            return result
        } finally {
            if (!completed) {
                runCatching { client.close() }
                    .onFailure { logger.warn("Failed to close MCP client after discovery failure", it) }
            }
        }
    }

    private class ClientCallbacks(
        val client: McpSyncClient,
        val callbacks: List<ToolCallback>,
    ) {
        var leases = 0
        var closed = false
    }

    private fun buildTransport(
        baseUrl: String,
        endpoint: String,
        authorization: AuthenticationRequestDto,
        transportType: McpClientTransportType,
    ): McpClientTransport {
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

    private fun requiredToken(authorization: AuthenticationRequestDto): String =
        authorization.token?.takeIf { it.isNotBlank() }
            ?: throw BadRequestException("A non-empty token is required for ${authorization.type} authentication")

    private fun requiredHeaderName(authorization: AuthenticationRequestDto): String =
        authorization.headerName?.takeIf { it.isNotBlank() }
            ?: throw BadRequestException("A non-empty header name is required for CUSTOM_HEADER authentication")
}
