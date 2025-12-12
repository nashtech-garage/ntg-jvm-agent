package com.ntgjvmagent.orchestrator.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.ntgjvmagent.orchestrator.dto.internal.ToolDataDto
import com.ntgjvmagent.orchestrator.dto.request.AuthenticationRequestDto
import com.ntgjvmagent.orchestrator.dto.request.ToolRequestDto
import com.ntgjvmagent.orchestrator.dto.response.ToolResponseDto
import com.ntgjvmagent.orchestrator.exception.BadRequestException
import com.ntgjvmagent.orchestrator.mapper.ToolMapper
import com.ntgjvmagent.orchestrator.repository.ToolRepository
import com.ntgjvmagent.orchestrator.utils.AuthType
import com.ntgjvmagent.orchestrator.utils.Constant
import com.ntgjvmagent.orchestrator.utils.McpClientTransportType
import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport
import io.modelcontextprotocol.spec.McpClientTransport
import io.modelcontextprotocol.spec.McpError
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider
import org.springframework.ai.tool.ToolCallback
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.http.HttpRequest
import java.util.UUID

@Service
class ToolService(
    private val repo: ToolRepository,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(ToolService::class.java)

    @Transactional(readOnly = true)
    fun getAllActive(): List<ToolResponseDto> = repo.findAllByActiveTrue().map(ToolMapper::toResponse)

    @Transactional(readOnly = true)
    fun getById(id: UUID): ToolResponseDto {
        val entity =
            repo.findByIdOrNull(id)
                ?: throw EntityNotFoundException("Tool not found: $id")
        return ToolMapper.toResponse(entity)
    }

    @Transactional
    fun create(request: ToolRequestDto) {
        val toolCallback =
            try {
                initializeToolCallback(request.baseUrl, request.endpoint, request.authorization)
            } catch (e: McpError) {
                logger.error(e.message)
                throw BadRequestException("Verification failed, MCP server info is incorrect")
            }
        insertTool(toolCallback, request)
    }

    @Transactional
    fun update(
        id: UUID,
        request: ToolDataDto,
    ): ToolResponseDto {
        val existing =
            repo.findByIdOrNull(id)
                ?: throw EntityNotFoundException("Tool not found: $id")

        existing.apply {
            name = request.name
            type = request.type
            description = request.description
            definition = request.definition
            active = request.active
        }
        return ToolMapper.toResponse(repo.save(existing))
    }

    @Transactional
    fun softDelete(id: UUID) {
        val tool =
            repo.findByIdOrNull(id)
                ?: throw EntityNotFoundException("Tool not found: $id")
        tool.markDeleted()
        repo.save(tool)
    }

    fun insertTool(
        toolCallbacks: List<ToolCallback>,
        request: ToolRequestDto,
    ) {
        if (toolCallbacks.isEmpty()) {
            return
        }

        val allTools = repo.findAll()
        for (toolCallback in toolCallbacks) {
            val toolDefinition = toolCallback.toolDefinition
            val toolName = toolDefinition.name()
            val inactiveToolMatchNames = allTools.stream().filter { it.name == toolName && !it.active }.toList()
            if (inactiveToolMatchNames.isNotEmpty()) {
                val reactiveToolMatchNames =
                    inactiveToolMatchNames.map {
                        it.active = true
                        it
                    }
                repo.saveAll(reactiveToolMatchNames)
                continue
            }

            val definition =
                objectMapper
                    .readValue(
                        toolDefinition.inputSchema(),
                        object : TypeReference<Map<String, Any>>() {},
                    )
            val connectionConfig =
                objectMapper
                    .convertValue(
                        request,
                        object : TypeReference<MutableMap<String, Any>>() {},
                    ).apply {
                        remove("baseUrl")
                    }
            val toolEntity =
                ToolMapper
                    .toEntity(
                        ToolDataDto(
                            toolName,
                            Constant.MCP_TOOL_TYPE,
                            request.baseUrl,
                            toolDefinition.description(),
                            definition,
                            connectionConfig,
                        ),
                    )
            repo.save(toolEntity)
        }
    }

    fun loadExternalToolCallbackFromDb(): List<ToolCallback> {
        val externalTools = repo.findActiveExternalTools()
        val toolCallbacks: MutableList<ToolCallback> = mutableListOf()
        for (tool in externalTools) {
            val connectionConfig = tool.getConfig()
            // Current only support SSE
            if (McpClientTransportType.SSE.name != connectionConfig["transportType"]) {
                continue
            }

            val authorization =
                objectMapper
                    .convertValue(
                        connectionConfig["authorization"],
                        object : TypeReference<AuthenticationRequestDto>() {},
                    )

            val tools =
                try {
                    initializeToolCallback(tool.getBaseUrl(), connectionConfig["endpoint"] as String, authorization)
                } catch (e: McpError) {
                    logger.error(e.message)
                    return emptyList()
                }
            toolCallbacks.addAll(tools)
        }

        return toolCallbacks
    }

    private fun initializeToolCallback(
        baseUrl: String,
        endpoint: String,
        authorization: AuthenticationRequestDto,
    ): List<ToolCallback> {
        val transport = buildHttpClientTransport(baseUrl, endpoint, authorization)
        val mcpSyncClient =
            McpClient
                .sync(
                    transport,
                ).build()
        mcpSyncClient.initialize()
        val mcpSyncClientList: MutableList<McpSyncClient> = mutableListOf(mcpSyncClient)
        val mcpToolCallbacks = SyncMcpToolCallbackProvider.syncToolCallbacks(mcpSyncClientList)
        return mcpToolCallbacks
    }

    private fun buildHttpClientTransport(
        baseUrl: String,
        endpoint: String,
        authorization: AuthenticationRequestDto,
    ): McpClientTransport {
        val customRequestBuilder =
            HttpRequest
                .newBuilder()
                .header("content-type", "application/json")
                .header("accept", "text/event-stream")

        val authMap =
            when (authorization.type) {
                AuthType.BEARER -> mapOf("Authorization" to "Bearer ${authorization.token}")
                AuthType.API_KEY -> mapOf("X-API-Key" to authorization.token)
                AuthType.CUSTOM_HEADER -> mapOf(authorization.headerName to authorization.token)
                else -> emptyMap<String, String>()
            }

        if (authMap.isNotEmpty()) {
            customRequestBuilder.header(authMap.keys.first(), authMap.values.first())
        }

        return HttpClientSseClientTransport
            .builder(baseUrl)
            .sseEndpoint(endpoint)
            .requestBuilder(customRequestBuilder)
            .build()
    }
}
