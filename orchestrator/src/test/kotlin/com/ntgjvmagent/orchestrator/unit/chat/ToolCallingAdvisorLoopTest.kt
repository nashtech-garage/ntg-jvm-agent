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
import com.ntgjvmagent.orchestrator.dto.response.AgentResponseDto
import com.ntgjvmagent.orchestrator.repository.AgentToolRepository
import com.ntgjvmagent.orchestrator.service.DynamicChatModelService
import com.ntgjvmagent.orchestrator.token.accounting.TokenMeteringService
import com.ntgjvmagent.orchestrator.tool.LocalToolCatalog
import com.ntgjvmagent.orchestrator.tool.SupportPolicyTool
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationHandler
import io.micrometer.observation.ObservationRegistry
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.ToolResponseMessage
import org.springframework.ai.chat.metadata.ChatGenerationMetadata
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.model.tool.ToolCallingChatOptions
import org.springframework.ai.tool.ToolCallback
import org.springframework.ai.tool.definition.ToolDefinition
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

        val answer =
            try {
                createFactory(model, observationRegistry)
                    .create(agentId)
                    .prompt()
                    .advisors(loggingAdvisor, observingAdvisor, reasoningAdvisor)
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
        assertEquals(2, model.prompts.size)
        assertTrue(
            model.prompts
                .first()
                .contents
                .contains(CANONICAL_PROMPT),
        )
        assertTrue(model.receivedToolResult.contains("\"initialResponseMinutes\":15"))
        assertTrue(observationNames.contains("spring.ai.tool"))

        assertEquals(2, messages.count { it == "Tool loop model stage started" })
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
                "I need the support policy before answering. ",
                "The tool result contains the response target.",
            ),
            reasoningEvents,
        )
    }

    private fun productionToolCallbacks(localToolCatalog: LocalToolCatalog): List<ToolCallback> {
        val agentToolRepository = mockk<AgentToolRepository>()
        every { agentToolRepository.findByAgentId(agentId) } returns emptyList()

        val unassignedDefinition = mockk<ToolDefinition>()
        every { unassignedDefinition.name() } returns "unassignedExternalTool"
        val unassignedCallback = mockk<ToolCallback>()
        every { unassignedCallback.toolDefinition } returns unassignedDefinition

        val globalToolCallbackProvider = mockk<GlobalToolCallbackProvider>()
        every { globalToolCallbackProvider.getToolCallbacks() } returns listOf(unassignedCallback)

        val callbacks =
            ToolExecutionFacade(
                agentToolRepository,
                FilteredToolCallbackProvider(),
                globalToolCallbackProvider,
                localToolCatalog,
                mockk<TokenMeteringService>(relaxed = true),
            ).createToolCallbacks(userId, agentId, "tool-loop-contract")

        assertEquals(listOf(SupportPolicyTool.TOOL_NAME), callbacks.map { it.toolDefinition.name() })
        assertFalse(callbacks.any { it.toolDefinition.name() == "unassignedExternalTool" })
        return callbacks
    }

    private fun createFactory(
        model: ChatModel,
        observationRegistry: ObservationRegistry,
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
            ToolCallingConfig().toolCallingAdvisorBuilder(observationRegistry),
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
        var receivedToolResult = ""

        override fun call(prompt: Prompt): ChatResponse = responseFor(prompt)

        override fun stream(prompt: Prompt): Flux<ChatResponse> = Flux.just(responseFor(prompt))

        override fun getOptions() = ToolCallingChatOptions.builder().build()

        private fun responseFor(prompt: Prompt): ChatResponse {
            prompts.add(prompt)
            optionTypes.add(prompt.options?.javaClass?.name ?: "null")
            val toolResponse = prompt.instructions.filterIsInstance<ToolResponseMessage>().singleOrNull()

            if (toolResponse == null) {
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

            receivedToolResult = toolResponse.responses.single().responseData()
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

    companion object {
        private const val CANONICAL_PROMPT =
            "Under the NTG support policy, what is the initial response target for a PREMIUM customer with a P1 incident?"

        private val logger =
            LoggerFactory.getLogger(ToolLoopLoggingAdvisor::class.java) as Logger
    }
}
