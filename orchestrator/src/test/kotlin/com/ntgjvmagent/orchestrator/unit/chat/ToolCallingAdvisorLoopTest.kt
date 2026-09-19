package com.ntgjvmagent.orchestrator.unit.chat

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.ntgjvmagent.orchestrator.advisor.ModelReasoningObservingAdvisor
import com.ntgjvmagent.orchestrator.advisor.ToolCallEvent
import com.ntgjvmagent.orchestrator.advisor.ToolCallObservingAdvisor
import com.ntgjvmagent.orchestrator.advisor.ToolLoopLoggingAdvisor
import com.ntgjvmagent.orchestrator.component.AgentChatClientFactory
import com.ntgjvmagent.orchestrator.component.FilteredToolCallbackProvider
import com.ntgjvmagent.orchestrator.component.GlobalToolCallbackProvider
import com.ntgjvmagent.orchestrator.component.ToolExecutionFacade
import com.ntgjvmagent.orchestrator.config.ToolCallingConfig
import com.ntgjvmagent.orchestrator.config.ToolSearchIndexProperties
import com.ntgjvmagent.orchestrator.dto.response.AgentResponseDto
import com.ntgjvmagent.orchestrator.entity.Tool
import com.ntgjvmagent.orchestrator.entity.agent.AgentTool
import com.ntgjvmagent.orchestrator.repository.AgentToolRepository
import com.ntgjvmagent.orchestrator.service.DynamicChatModelService
import com.ntgjvmagent.orchestrator.token.MeteredToolCallback
import com.ntgjvmagent.orchestrator.token.accounting.TokenMeteringService
import com.ntgjvmagent.orchestrator.tool.LocalToolCatalog
import com.ntgjvmagent.orchestrator.tool.LocalToolCatalog.Companion.TODO_WRITE_TOOL_NAME
import com.ntgjvmagent.orchestrator.tool.SupportPolicyTool
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationHandler
import io.micrometer.observation.ObservationRegistry
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.ToolResponseMessage
import org.springframework.ai.chat.metadata.ChatGenerationMetadata
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.model.ToolContext
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.model.tool.ToolCallingChatOptions
import org.springframework.ai.support.ToolCallbacks
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.definition.ToolDefinition
import org.springframework.ai.tool.toolsearch.ToolIndex
import org.springframework.ai.tool.toolsearch.ToolReference
import org.springframework.ai.tool.toolsearch.ToolSearchRequest
import org.springframework.ai.tool.toolsearch.ToolSearchResponse
import org.springframework.ai.tool.toolsearch.index.regex.RegexToolIndex
import reactor.core.publisher.Flux
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToolCallingAdvisorLoopTest {
    private val agentId = UUID.randomUUID()
    private val userId = UUID.randomUUID()

    @Test
    fun `ChatClient executes the local tool and exposes every loop stage`() {
        val observationNames = mutableListOf<String>()
        val observationRegistry = recordingObservationRegistry(observationNames)
        val model = SupportToolCallingModel()
        val localToolCatalog = LocalToolCatalog(SupportPolicyTool())
        val tools = productionToolCallbacks(localToolCatalog)
        val loggingAdvisor = ToolLoopLoggingAdvisor()
        val toolCallEvents = mutableListOf<ToolCallEvent>()
        val observingAdvisor = ToolCallObservingAdvisor(toolCallEvents::add)
        val reasoningEvents = mutableListOf<String>()
        val reasoningAdvisor = ModelReasoningObservingAdvisor(reasoningEvents::add)
        val logAppender = attachLogAppender()
        val toolIndex = RecordingToolIndex(RegexToolIndex())

        val answer =
            try {
                createFactory(model, observationRegistry, toolIndex)
                    .create(agentId)
                    .prompt()
                    .advisors(loggingAdvisor, observingAdvisor, reasoningAdvisor)
                    .advisors { it.param(ChatMemory.CONVERSATION_ID, TOOL_SEARCH_SESSION_ID) }
                    .tools(*tools.toTypedArray())
                    .user(CANONICAL_PROMPT)
                    .stream()
                    .content()
                    .collectList()
                    .block()
                    .orEmpty()
                    .joinToString("")
            } finally {
                detachLogAppender(logAppender)
            }

        val messages = logAppender.list.map { it.formattedMessage }
        assertEquals(
            "The initial response target is 15 minutes.",
            answer,
            "prompts=${model.prompts.size}, options=${model.optionTypes}, toolResult=${model.receivedToolResult}, logs=$messages",
        )
        assertEquals(3, model.prompts.size)
        assertTrue(
            model.prompts
                .first()
                .contents
                .contains(CANONICAL_PROMPT),
        )
        assertEquals(listOf(TOOL_SEARCH_TOOL_NAME), model.availableToolNames.first())
        assertTrue(
            model.availableToolNames[1].containsAll(
                listOf(TOOL_SEARCH_TOOL_NAME, SupportPolicyTool.TOOL_NAME),
            ),
        )
        assertEquals(listOf(TOOL_SEARCH_TOOL_NAME), model.availableToolNames.last())
        assertTrue(model.receivedSearchResult.contains(SupportPolicyTool.TOOL_NAME))
        assertTrue(model.receivedToolResult.contains("\"initialResponseMinutes\":15"))
        assertTrue(observationNames.contains("spring.ai.tool"))
        assertEquals(
            setOf(SupportPolicyTool.TOOL_NAME, TODO_WRITE_TOOL_NAME, ASSIGNED_EXTERNAL_TOOL),
            toolIndex.indexedToolNames.toSet(),
        )
        assertFalse(toolIndex.indexedToolNames.contains(UNASSIGNED_EXTERNAL_TOOL))

        assertEquals(3, messages.count { it == "Tool loop model stage started" })
        assertTrue(
            messages.any {
                it.contains("requestedToolCount=1") &&
                    it.contains(SupportPolicyTool.TOOL_NAME)
            },
        )
        assertTrue(messages.any { it.contains("requestedToolCount=0") })
        assertEquals(
            listOf(
                ToolCallEvent(
                    id = "tool-search-call-1",
                    name = TOOL_SEARCH_TOOL_NAME,
                    phase = ToolCallEvent.Phase.STARTED,
                ),
                ToolCallEvent(
                    id = "tool-search-call-1",
                    name = TOOL_SEARCH_TOOL_NAME,
                    phase = ToolCallEvent.Phase.COMPLETED,
                ),
                ToolCallEvent(
                    id = "support-policy-call-1",
                    name = SupportPolicyTool.TOOL_NAME,
                    phase = ToolCallEvent.Phase.STARTED,
                ),
                ToolCallEvent(
                    id = "support-policy-call-1",
                    name = SupportPolicyTool.TOOL_NAME,
                    phase = ToolCallEvent.Phase.COMPLETED,
                ),
            ),
            toolCallEvents,
        )
        assertEquals(
            listOf(
                "I need to discover the support capability. ",
                "I need the support policy before answering. ",
                "The tool result contains the response target.",
            ),
            reasoningEvents,
        )
    }

    @Test
    fun `tool index is reused across clients for the same session and tool fingerprint`() {
        val toolIndex = RecordingToolIndex(RegexToolIndex())
        val factory = createFactory(FinalAnswerModel(), ObservationRegistry.NOOP, toolIndex)
        val tools = ToolCallbacks.from(SupportPolicyTool())

        repeat(2) {
            factory
                .create(agentId)
                .prompt()
                .advisors { it.param(ChatMemory.CONVERSATION_ID, TOOL_SEARCH_SESSION_ID) }
                .tools(*tools)
                .user("Acknowledge")
                .call()
                .content()
        }

        assertEquals(1, toolIndex.indexBatches)
    }

    @Test
    fun `natural current-time question discovers and invokes the datetime tool`() {
        val model = CurrentTimeToolCallingModel()
        val datetimeTool = callback(DATETIME_TOOL_NAME, DATETIME_TOOL_DESCRIPTION, DATETIME_TOOL_RESULT)

        val answer =
            createFactory(model, ObservationRegistry.NOOP, RegexToolIndex())
                .create(agentId)
                .prompt()
                .advisors { it.param(ChatMemory.CONVERSATION_ID, "current-time-session") }
                .tools(datetimeTool)
                .user("What time is it?")
                .call()
                .content()

        assertEquals("The current UTC time is 2026-09-23T00:00:00Z.", answer)
        assertTrue(model.systemPrompt.contains("You MUST search before answering questions about the current date"))
        assertTrue(model.receivedSearchResult.contains(DATETIME_TOOL_NAME))
        assertEquals(DATETIME_TOOL_RESULT, model.receivedDatetimeResult)
    }

    private fun productionToolCallbacks(localToolCatalog: LocalToolCatalog): List<ToolCallback> {
        val agentToolRepository = mockk<AgentToolRepository>()
        val assignedTool = Tool(name = ASSIGNED_EXTERNAL_TOOL)
        every { agentToolRepository.findByAgentId(agentId) } returns
            listOf(mockk<AgentTool> { every { tool } returns assignedTool })

        val assignedCallback = callback(ASSIGNED_EXTERNAL_TOOL, "Read the assigned billing status")
        val unassignedCallback = callback(UNASSIGNED_EXTERNAL_TOOL, "Read another agent's private status")

        val globalToolCallbackProvider = mockk<GlobalToolCallbackProvider>()
        every { globalToolCallbackProvider.getToolCallbacks() } returns
            listOf(assignedCallback, unassignedCallback)

        val callbacks =
            ToolExecutionFacade(
                agentToolRepository,
                FilteredToolCallbackProvider(),
                globalToolCallbackProvider,
                localToolCatalog,
                mockk<TokenMeteringService>(relaxed = true),
            ).createToolCallbacks(userId, agentId, "tool-loop-contract")

        assertEquals(
            setOf(SupportPolicyTool.TOOL_NAME, TODO_WRITE_TOOL_NAME, ASSIGNED_EXTERNAL_TOOL),
            callbacks.map { it.toolDefinition.name() }.toSet(),
        )
        assertTrue(callbacks.all { it is MeteredToolCallback })
        assertFalse(callbacks.any { it.toolDefinition.name() == UNASSIGNED_EXTERNAL_TOOL })
        return callbacks
    }

    private fun callback(
        name: String,
        description: String,
        result: String = "ok",
    ): ToolCallback {
        val definition = mockk<ToolDefinition>()
        every { definition.name() } returns name
        every { definition.description() } returns description
        every { definition.inputSchema() } returns "{}"
        return mockk(relaxed = true) {
            every { toolDefinition } returns definition
            every { call(any()) } returns result
            every { call(any<String>(), any<ToolContext>()) } returns result
        }
    }

    private fun createFactory(
        model: ChatModel,
        observationRegistry: ObservationRegistry,
        toolIndex: ToolIndex,
    ): AgentChatClientFactory {
        val agentConfig = mockk<AgentResponseDto> { every { this@mockk.model } returns "tool-contract-model" }
        val dynamicChatModelService =
            mockk<DynamicChatModelService> {
                every { getChatModel(agentId) } returns model
                every { getAgentConfig(agentId) } returns agentConfig
            }
        return AgentChatClientFactory(
            dynamicChatModelService,
            observationRegistry,
            ToolCallingConfig().toolCallingAdvisorBuilder(observationRegistry, toolIndex, ToolSearchIndexProperties()),
        )
    }

    private fun recordingObservationRegistry(observationNames: MutableList<String>): ObservationRegistry =
        ObservationRegistry.create().also { registry ->
            registry.observationConfig().observationHandler(
                object : ObservationHandler<Observation.Context> {
                    override fun onStop(context: Observation.Context) {
                        context.name?.let(observationNames::add)
                    }

                    override fun supportsContext(context: Observation.Context): Boolean = true
                },
            )
        }

    private fun attachLogAppender(): ListAppender<ILoggingEvent> {
        val appender = ListAppender<ILoggingEvent>()
        appender.start()
        logger.addAppender(appender)
        logger.level = ch.qos.logback.classic.Level.DEBUG
        return appender
    }

    private fun detachLogAppender(appender: ListAppender<ILoggingEvent>) {
        logger.detachAppender(appender)
        appender.stop()
    }

    private class SupportToolCallingModel : ChatModel {
        val prompts = mutableListOf<Prompt>()
        val optionTypes = mutableListOf<String>()
        val availableToolNames = mutableListOf<List<String>>()
        var receivedSearchResult = ""
        var receivedToolResult = ""

        override fun call(prompt: Prompt): ChatResponse = responseFor(prompt)

        override fun stream(prompt: Prompt): Flux<ChatResponse> = Flux.just(responseFor(prompt))

        override fun getOptions() = ToolCallingChatOptions.builder().build()

        private fun responseFor(prompt: Prompt): ChatResponse {
            prompts.add(prompt)
            optionTypes.add(prompt.options?.javaClass?.name ?: "null")
            availableToolNames +=
                (prompt.options as ToolCallingChatOptions)
                    .toolCallbacks
                    .orEmpty()
                    .map { it.toolDefinition.name() }
                    .sorted()
            val toolResponse = prompt.instructions.filterIsInstance<ToolResponseMessage>().lastOrNull()

            if (toolResponse == null) {
                val toolCall =
                    AssistantMessage.ToolCall(
                        "tool-search-call-1",
                        "function",
                        TOOL_SEARCH_TOOL_NAME,
                        """{"query":"support policy response target","maxResults":2}""",
                    )
                return ChatResponse(
                    listOf(
                        Generation(
                            AssistantMessage
                                .builder()
                                .content("")
                                .properties(
                                    mapOf(
                                        "reasoningContent" to
                                            "I need to discover the support capability. ",
                                    ),
                                ).toolCalls(listOf(toolCall))
                                .build(),
                            ChatGenerationMetadata.builder().finishReason("tool_calls").build(),
                        ),
                    ),
                )
            }

            val response = toolResponse.responses.single()
            if (response.name() == TOOL_SEARCH_TOOL_NAME) {
                receivedSearchResult = response.responseData()
                val toolCall =
                    AssistantMessage.ToolCall(
                        "support-policy-call-1",
                        "function",
                        SupportPolicyTool.TOOL_NAME,
                        """{"customerTier":"PREMIUM","incidentPriority":"P1"}""",
                    )
                return ChatResponse(
                    listOf(
                        Generation(
                            AssistantMessage
                                .builder()
                                .content("")
                                .properties(
                                    mapOf(
                                        "reasoningContent" to
                                            "I need the support policy before answering. ",
                                    ),
                                ).toolCalls(listOf(toolCall))
                                .build(),
                            ChatGenerationMetadata.builder().finishReason("tool_calls").build(),
                        ),
                    ),
                )
            }

            receivedToolResult = response.responseData()
            return ChatResponse(
                listOf(
                    Generation(
                        AssistantMessage("The initial response target is 15 minutes."),
                        ChatGenerationMetadata
                            .builder()
                            .finishReason("stop")
                            .metadata(
                                "thinking",
                                "The tool result contains the response target.",
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
        var indexBatches = 0

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
            indexBatches++
            indexedToolNames += toolReferences.map { it.toolName() }
            delegate.indexTools(sessionId, toolReferences)
        }

        override fun search(request: ToolSearchRequest): ToolSearchResponse = delegate.search(request)

        override fun clearIndex(sessionId: String) = delegate.clearIndex(sessionId)
    }

    private class FinalAnswerModel : ChatModel {
        override fun call(prompt: Prompt): ChatResponse =
            ChatResponse(listOf(Generation(AssistantMessage("acknowledged"))))

        override fun stream(prompt: Prompt): Flux<ChatResponse> = Flux.just(call(prompt))

        override fun getOptions() = ToolCallingChatOptions.builder().build()
    }

    private class CurrentTimeToolCallingModel : ChatModel {
        var systemPrompt = ""
        var receivedSearchResult = ""
        var receivedDatetimeResult = ""

        override fun call(prompt: Prompt): ChatResponse = responseFor(prompt)

        override fun stream(prompt: Prompt): Flux<ChatResponse> = Flux.just(responseFor(prompt))

        override fun getOptions() = ToolCallingChatOptions.builder().build()

        private fun responseFor(prompt: Prompt): ChatResponse {
            systemPrompt =
                prompt.instructions
                    .filterIsInstance<SystemMessage>()
                    .joinToString("\n") { it.text.orEmpty() }
            val toolResponse = prompt.instructions.filterIsInstance<ToolResponseMessage>().lastOrNull()

            if (toolResponse == null) {
                val toolCall =
                    AssistantMessage.ToolCall(
                        "datetime-search-call",
                        "function",
                        TOOL_SEARCH_TOOL_NAME,
                        """{"query":"current UTC datetime","maxResults":2}""",
                    )
                return toolCallResponse(toolCall)
            }

            val response = toolResponse.responses.single()
            if (response.name() == TOOL_SEARCH_TOOL_NAME) {
                receivedSearchResult = response.responseData()
                val toolCall =
                    AssistantMessage.ToolCall(
                        "datetime-call",
                        "function",
                        DATETIME_TOOL_NAME,
                        "{}",
                    )
                return toolCallResponse(toolCall)
            }

            receivedDatetimeResult = response.responseData()
            return ChatResponse(
                listOf(
                    Generation(
                        AssistantMessage("The current UTC time is 2026-09-23T00:00:00Z."),
                        ChatGenerationMetadata.builder().finishReason("stop").build(),
                    ),
                ),
            )
        }

        private fun toolCallResponse(toolCall: AssistantMessage.ToolCall): ChatResponse =
            ChatResponse(
                listOf(
                    Generation(
                        AssistantMessage
                            .builder()
                            .content("")
                            .toolCalls(listOf(toolCall))
                            .build(),
                        ChatGenerationMetadata.builder().finishReason("tool_calls").build(),
                    ),
                ),
            )
    }

    companion object {
        private const val CANONICAL_PROMPT =
            "Under the NTG support policy, what is the initial response target for a PREMIUM customer with a P1 incident?"
        private const val TOOL_SEARCH_SESSION_ID = "tool-loop-session"
        private const val TOOL_SEARCH_TOOL_NAME = "toolSearchTool"
        private const val DATETIME_TOOL_NAME = "getCurrentDatetime"
        private const val DATETIME_TOOL_DESCRIPTION = "Return the current UTC datetime as an ISO-8601 timestamp"
        private const val DATETIME_TOOL_RESULT = "{\"datetimeUtc\":\"2026-09-23T00:00:00Z\"}"
        private const val ASSIGNED_EXTERNAL_TOOL = "assignedExternalTool"
        private const val UNASSIGNED_EXTERNAL_TOOL = "unassignedExternalTool"

        private val logger =
            LoggerFactory.getLogger(ToolLoopLoggingAdvisor::class.java) as Logger
    }
}
