package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.config.McpDynamicClientProperties
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
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider
import org.springframework.ai.tool.ToolCallback
import org.springframework.stereotype.Component
import java.net.http.HttpRequest
import java.security.MessageDigest
import java.util.HexFormat
import java.util.concurrent.ConcurrentHashMap

interface McpToolDiscovery {
    fun discover(
        baseUrl: String,
        endpoint: String,
        authorization: AuthenticationRequestDto,
        transportType: McpClientTransportType,
    ): List<ToolCallback>
}

@Component
class DefaultMcpSyncClientFactory(
    private val properties: McpDynamicClientProperties,
) : McpSyncClientFactory {
    override fun create(transport: McpClientTransport): McpSyncClient {
        require(!properties.initializationTimeout.isZero && !properties.initializationTimeout.isNegative) {
            "MCP initialization timeout must be positive"
        }
        require(!properties.requestTimeout.isZero && !properties.requestTimeout.isNegative) {
            "MCP request timeout must be positive"
        }

        return McpClient
            .sync(transport)
            .initializationTimeout(properties.initializationTimeout)
            .requestTimeout(properties.requestTimeout)
            .build()
    }
}

fun interface McpSyncClientFactory {
    fun create(transport: McpClientTransport): McpSyncClient
}

@Component
class HttpMcpToolDiscovery(
    private val clientFactory: McpSyncClientFactory,
) : McpToolDiscovery {
    private val connections = ConcurrentHashMap<McpConnectionKey, ManagedMcpConnection>()

    override fun discover(
        baseUrl: String,
        endpoint: String,
        authorization: AuthenticationRequestDto,
        transportType: McpClientTransportType,
    ): List<ToolCallback> {
        val key = McpConnectionKey.from(baseUrl, endpoint, authorization, transportType)
        val connection =
            connections.computeIfAbsent(key) {
                createConnection(baseUrl, endpoint, authorization, transportType)
            }

        return runCatching {
            SyncMcpToolCallbackProvider.syncToolCallbacks(listOf(connection.client))
        }.getOrElse { exception ->
            if (connections.remove(key, connection)) {
                runCatching { connection.client.close() }
            }
            throw exception
        }
    }

    @PreDestroy
    fun close() {
        connections.values.forEach { connection ->
            runCatching { connection.client.close() }
        }
        connections.clear()
    }

    private fun createConnection(
        baseUrl: String,
        endpoint: String,
        authorization: AuthenticationRequestDto,
        transportType: McpClientTransportType,
    ): ManagedMcpConnection {
        val transport = buildTransport(baseUrl, endpoint, authorization, transportType)
        val client = clientFactory.create(transport)
        return runCatching {
            client.initialize()
            ManagedMcpConnection(client)
        }.getOrElse { exception ->
            runCatching { client.close() }
            throw exception
        }
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

    private data class ManagedMcpConnection(
        val client: McpSyncClient,
    )

    private data class McpConnectionKey(
        val baseUrl: String,
        val endpoint: String,
        val authType: AuthType,
        val tokenFingerprint: String?,
        val headerName: String?,
        val transportType: McpClientTransportType,
    ) {
        companion object {
            fun from(
                baseUrl: String,
                endpoint: String,
                authorization: AuthenticationRequestDto,
                transportType: McpClientTransportType,
            ) = McpConnectionKey(
                baseUrl = baseUrl.trimEnd('/'),
                endpoint = endpoint,
                authType = authorization.type,
                tokenFingerprint = authorization.token?.sha256(),
                headerName = authorization.headerName,
                transportType = transportType,
            )

            private fun String.sha256(): String =
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(toByteArray()))
        }
    }
}
