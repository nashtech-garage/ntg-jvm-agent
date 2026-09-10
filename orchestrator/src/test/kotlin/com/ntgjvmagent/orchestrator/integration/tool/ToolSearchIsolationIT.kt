package com.ntgjvmagent.orchestrator.integration.tool

import com.ntgjvmagent.orchestrator.component.FilteredToolCallbackProvider
import com.ntgjvmagent.orchestrator.component.GlobalToolCallbackProvider
import com.ntgjvmagent.orchestrator.component.ToolExecutionFacade
import com.ntgjvmagent.orchestrator.config.ToolCallingConfig
import com.ntgjvmagent.orchestrator.config.ToolSearchIndexProperties
import com.ntgjvmagent.orchestrator.entity.Tool
import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.entity.agent.AgentTool
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.model.ProviderType
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import com.ntgjvmagent.orchestrator.repository.AgentToolRepository
import com.ntgjvmagent.orchestrator.repository.ToolRepository
import com.ntgjvmagent.orchestrator.token.MeteredToolCallback
import com.ntgjvmagent.orchestrator.token.accounting.TokenMeteringService
import com.ntgjvmagent.orchestrator.tool.LocalToolCatalog
import com.ntgjvmagent.orchestrator.tool.SupportPolicyTool
import com.ntgjvmagent.orchestrator.utils.Constant
import io.micrometer.observation.ObservationRegistry
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.ToolResponseMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.model.tool.ToolCallingChatOptions
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.definition.ToolDefinition
import org.springframework.ai.tool.toolsearch.ToolIndex
import org.springframework.ai.tool.toolsearch.ToolReference
import org.springframework.ai.tool.toolsearch.ToolSearchRequest
import org.springframework.ai.tool.toolsearch.ToolSearchResponse
import org.springframework.ai.tool.toolsearch.index.regex.RegexToolIndex
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Transactional
class ToolSearchIsolationIT
    @Autowired
    constructor(
        private val agentRepository: AgentRepository,
        private val toolRepository: ToolRepository,
        private val agentToolRepository: AgentToolRepository,
    ) : BaseIntegrationTest() {
        @Test
        fun `agent cannot discover a tool assigned only to another agent`() {
            val agentA = agentRepository.save(agent("tool-search-agent-a"))
            val agentB = agentRepository.save(agent("tool-search-agent-b"))
            val toolA =
                toolRepository.save(
                    Tool(
                        name = AGENT_A_TOOL,
                        type = Constant.MCP_TOOL_TYPE,
                        description = "Read agent A billing status",
                    ),
                )
            val toolB =
                toolRepository.save(
                    Tool(
                        name = AGENT_B_TOOL,
                        type = Constant.MCP_TOOL_TYPE,
                        description = "Read agent B private status",
                    ),
                )
            agentToolRepository.save(AgentTool.of(agentA, toolA))
            agentToolRepository.save(AgentTool.of(agentB, toolB))
            agentToolRepository.flush()

            val callbacks = callbacksFor(requireNotNull(agentA.id), toolA, toolB)
            val toolIndex = RecordingToolIndex(RegexToolIndex())
            val model = IsolationProbeModel()
            val advisor =
                ToolCallingConfig()
                    .toolCallingAdvisorBuilder(ObservationRegistry.NOOP, toolIndex, ToolSearchIndexProperties())
                    .build()

            val answer =
                ChatClient
                    .builder(model)
                    .defaultAdvisors(advisor)
                    .build()
                    .prompt()
                    .advisors { it.param(ChatMemory.CONVERSATION_ID, UUID.randomUUID().toString()) }
                    .tools(*callbacks.toTypedArray())
                    .user("Find agent B private status")
                    .call()
                    .content()

            assertFalse(answer.orEmpty().contains(AGENT_B_TOOL))
            assertEquals(setOf(SupportPolicyTool.TOOL_NAME, AGENT_A_TOOL), toolIndex.indexedToolNames.toSet())
            assertFalse(toolIndex.indexedToolNames.contains(AGENT_B_TOOL))
            assertTrue(callbacks.all { it is MeteredToolCallback })
        }

        private fun callbacksFor(
            agentId: UUID,
            toolA: Tool,
            toolB: Tool,
        ): List<ToolCallback> {
            val globalProvider = mockk<GlobalToolCallbackProvider>()
            every { globalProvider.getToolCallbacks() } returns
                listOf(
                    callback(toolA.name, requireNotNull(toolA.description)),
                    callback(toolB.name, requireNotNull(toolB.description)),
                )
            return ToolExecutionFacade(
                agentToolRepository = agentToolRepository,
                filteredToolCallbackProvider = FilteredToolCallbackProvider(),
                globalToolCallbackProvider = globalProvider,
                localToolCatalog = LocalToolCatalog(SupportPolicyTool()),
                tokenMeteringService = mockk<TokenMeteringService>(relaxed = true),
            ).createToolCallbacks(
                userId = UUID.randomUUID(),
                agentId = agentId,
                correlationId = "tool-search-isolation",
            )
        }

        private fun agent(name: String): Agent =
            Agent(
                name = "$name-${UUID.randomUUID()}",
                provider = ProviderType.OPENAI,
                baseUrl = "https://api.openai.com/v1",
                apiKey = "test-key",
                chatCompletionsPath = "/chat/completions",
                model = "test-model",
            )

        private fun callback(
            name: String,
            description: String,
        ): ToolCallback =
            object : ToolCallback {
                override fun getToolDefinition(): ToolDefinition =
                    ToolDefinition
                        .builder()
                        .name(name)
                        .description(description)
                        .inputSchema("{}")
                        .build()

                override fun call(arguments: String): String = "ok"
            }

        private class IsolationProbeModel : ChatModel {
            override fun call(prompt: Prompt): ChatResponse = response(prompt)

            override fun stream(prompt: Prompt): Flux<ChatResponse> = Flux.just(response(prompt))

            override fun getOptions() = ToolCallingChatOptions.builder().build()

            private fun response(prompt: Prompt): ChatResponse {
                val toolResponse = prompt.instructions.filterIsInstance<ToolResponseMessage>().lastOrNull()
                if (toolResponse != null) {
                    return ChatResponse(
                        listOf(Generation(AssistantMessage(toolResponse.responses.single().responseData()))),
                    )
                }
                return ChatResponse(
                    listOf(
                        Generation(
                            AssistantMessage
                                .builder()
                                .content("")
                                .toolCalls(
                                    listOf(
                                        AssistantMessage.ToolCall(
                                            "isolation-search-call",
                                            "function",
                                            TOOL_SEARCH_TOOL_NAME,
                                            """{"query":"agent B private status","maxResults":5}""",
                                        ),
                                    ),
                                ).build(),
                        ),
                    ),
                )
            }
        }

        private class RecordingToolIndex(
            private val delegate: ToolIndex,
        ) : ToolIndex {
            val indexedToolNames = mutableListOf<String>()

            override fun indexTool(
                sessionId: String,
                toolReference: ToolReference,
            ) {
                indexedToolNames += toolReference.toolName()
                delegate.indexTool(sessionId, toolReference)
            }

            override fun indexTools(
                sessionId: String,
                toolReferences: List<ToolReference>,
            ) {
                indexedToolNames += toolReferences.map { it.toolName() }
                delegate.indexTools(sessionId, toolReferences)
            }

            override fun search(request: ToolSearchRequest): ToolSearchResponse = delegate.search(request)

            override fun clearIndex(sessionId: String) = delegate.clearIndex(sessionId)
        }

        private companion object {
            const val TOOL_SEARCH_TOOL_NAME = "toolSearchTool"
            const val AGENT_A_TOOL = "agentABillingStatus"
            const val AGENT_B_TOOL = "agentBPrivateStatus"
        }
    }
