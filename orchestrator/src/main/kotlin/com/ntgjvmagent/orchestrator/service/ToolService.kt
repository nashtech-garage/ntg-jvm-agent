package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.dto.ExternalToolDto
import com.ntgjvmagent.orchestrator.dto.internal.ToolDataDto
import com.ntgjvmagent.orchestrator.dto.request.AuthenticationRequestDto
import com.ntgjvmagent.orchestrator.dto.request.ToolRequestDto
import com.ntgjvmagent.orchestrator.dto.response.ToolResponseDto
import com.ntgjvmagent.orchestrator.exception.BadRequestException
import com.ntgjvmagent.orchestrator.mapper.ToolMapper
import com.ntgjvmagent.orchestrator.repository.ToolRepository
import com.ntgjvmagent.orchestrator.utils.Constant
import com.ntgjvmagent.orchestrator.utils.McpClientTransportType
import io.modelcontextprotocol.spec.McpError
import io.modelcontextprotocol.spec.McpTransportException
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.ai.tool.ToolCallback
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import java.util.concurrent.CompletionException

private val logger = LoggerFactory.getLogger(ToolService::class.java)

@Service
class ToolService(
    private val repo: ToolRepository,
    private val objectMapper: ObjectMapper,
    private val mcpToolDiscovery: McpToolDiscovery,
) {
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
                mcpToolDiscovery.verify(
                    request.baseUrl,
                    request.endpoint,
                    request.authorization,
                    parseTransportType(request.transportType),
                )
            } catch (e: McpError) {
                verificationFailed(e)
            } catch (e: McpTransportException) {
                verificationFailed(e)
            } catch (e: CompletionException) {
                verificationFailed(e)
            } catch (e: IllegalStateException) {
                verificationFailed(e)
            } catch (e: IllegalArgumentException) {
                verificationFailed(e)
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

    fun loadExternalToolCallbackFromDb(): LeasedToolCallbacks {
        val connections = repo.findActiveExternalTools().mapNotNull(::parseExternalConnection)
        val leases = mutableListOf<LeasedToolCallbacks>()
        return runCatching {
            connections.mapNotNullTo(leases, ::discoverExternalTool)
            mcpToolDiscovery.retainActiveConnections(connections.toSet())
            LeasedToolCallbacks(leases.flatMap { it.callbacks }) { leases.forEach { it.close() } }
        }.getOrElse {
            leases.forEach { it.close() }
            throw it
        }
    }

    private fun parseExternalConnection(tool: ExternalToolDto): McpConnection? =
        runCatching {
            val connectionConfig = tool.getConfig()
            val transportType = parseTransportType(connectionConfig["transportType"] as String)
            val authorization =
                objectMapper.convertValue(
                    connectionConfig["authorization"],
                    object : TypeReference<AuthenticationRequestDto>() {},
                )
            McpConnection(
                tool.getBaseUrl(),
                connectionConfig["endpoint"] as String,
                authorization,
                transportType,
            )
        }.getOrElse {
            logger.warn("Skipping invalid persisted MCP connection: {}", it.javaClass.simpleName)
            null
        }

    private fun discoverExternalTool(connection: McpConnection): LeasedToolCallbacks? =
        runCatching {
            mcpToolDiscovery.discover(
                connection.baseUrl,
                connection.endpoint,
                connection.authorization(),
                connection.transportType,
            )
        }.getOrElse {
            logger.warn("Skipping unavailable MCP connection: {}", it.message)
            null
        }

    private fun parseTransportType(value: String): McpClientTransportType =
        runCatching { McpClientTransportType.valueOf(value.uppercase()) }
            .getOrElse { throw BadRequestException("Unsupported MCP transport type: $value") }
}

private fun verificationFailed(error: Throwable): Nothing {
    logger.warn("MCP verification failed: {}", error.javaClass.simpleName)
    throw BadRequestException("Verification failed, MCP server info is incorrect")
}
