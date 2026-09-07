package com.ntgjvmagent.orchestrator.integration.session

import com.ntgjvmagent.orchestrator.advisor.CallAdvisorRegistry
import com.ntgjvmagent.orchestrator.advisor.SuccessfulSessionRequestAdvisor
import com.ntgjvmagent.orchestrator.component.AgentChatClientFactory
import com.ntgjvmagent.orchestrator.component.ToolExecutionFacade
import com.ntgjvmagent.orchestrator.config.ChatReasoningProperties
import com.ntgjvmagent.orchestrator.config.ToolCallingConfig
import com.ntgjvmagent.orchestrator.dto.ChatRequestDto
import com.ntgjvmagent.orchestrator.dto.ChatResponseDto
import com.ntgjvmagent.orchestrator.dto.response.AgentResponseDto
import com.ntgjvmagent.orchestrator.entity.ChatMessage
import com.ntgjvmagent.orchestrator.entity.Conversation
import com.ntgjvmagent.orchestrator.exception.ResourceNotFoundException
import com.ntgjvmagent.orchestrator.exception.TokenLimitExceededException
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.integration.config.TestAuditorConfig
import com.ntgjvmagent.orchestrator.model.ChatMessageType
import com.ntgjvmagent.orchestrator.model.ChatStreamEvent
import com.ntgjvmagent.orchestrator.model.TokenOperation
import com.ntgjvmagent.orchestrator.repository.ChatMessageRepository
import com.ntgjvmagent.orchestrator.repository.ConversationRepository
import com.ntgjvmagent.orchestrator.service.ChatModelService
import com.ntgjvmagent.orchestrator.service.ChatStreamService
import com.ntgjvmagent.orchestrator.service.ConversationCommandService
import com.ntgjvmagent.orchestrator.service.ConversationSessionService
import com.ntgjvmagent.orchestrator.service.ConversationStreamingService
import com.ntgjvmagent.orchestrator.service.DynamicChatModelService
import com.ntgjvmagent.orchestrator.service.SummarizationService
import com.ntgjvmagent.orchestrator.token.accounting.TokenAccountingFacade
import com.ntgjvmagent.orchestrator.tool.SupportPolicyTool
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.ToolResponseMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.metadata.ChatGenerationMetadata
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.model.tool.ToolCallingChatOptions
import org.springframework.ai.session.CreateSessionRequest
import org.springframework.ai.session.MessageFilter
import org.springframework.ai.session.SessionService
import org.springframework.ai.session.advisor.IdempotentSessionEventIdGenerator
import org.springframework.ai.session.advisor.SessionMemoryAdvisor
import org.springframework.ai.support.ToolCallbacks
import org.springframework.beans.factory.annotation.Autowired
import reactor.core.publisher.Flux
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.util.UUID

class SessionMemoryIT : BaseIntegrationTest() {
    @Autowired
    private lateinit var sessionService: SessionService

    private lateinit var sessionMemoryAdvisor: SessionMemoryAdvisor

    private lateinit var successfulSessionRequestAdvisor: SuccessfulSessionRequestAdvisor

    private lateinit var sessionScheduler: Scheduler

    @Autowired
    private lateinit var conversationSessionService: ConversationSessionService

    @Autowired
    private lateinit var conversationRepository: ConversationRepository

    @Autowired
    private lateinit var messageRepository: ChatMessageRepository

    @BeforeAll
    fun configureSessionAdvisor() {
        sessionScheduler = Schedulers.newBoundedElastic(4, 100, "session-memory-test")
        val eventIdGenerator =
            IdempotentSessionEventIdGenerator(
                SuccessfulSessionRequestAdvisor.RUN_ID_CONTEXT_KEY,
                SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY,
            )
        sessionMemoryAdvisor =
            SessionMemoryAdvisor
                .builder(sessionService)
                .order(ToolCallingConfig.TOOL_CALLING_ADVISOR_ORDER + 1)
                .scheduler(sessionScheduler)
                .messageFilter(
                    MessageFilter
                        .byMessageType(org.springframework.ai.chat.messages.MessageType.ASSISTANT)
                        .and(MessageFilter.skipEmptyMessages()),
                ).responseEventIdGenerator(eventIdGenerator)
                .build()
        successfulSessionRequestAdvisor =
            SuccessfulSessionRequestAdvisor(sessionService, eventIdGenerator)
    }

    @AfterAll
    fun disposeSessionScheduler() {
        sessionScheduler.disposeGracefully().block()
    }

    @Test
    fun `turn three retains turn one with structured roles`() {
        val model = CapturingModel()
        val sessionId = UUID.randomUUID()
        val userId = TestAuditorConfig.TEST_USER_ID
        val client =
            ChatClient
                .builder(model)
                .defaultAdvisors(sessionMemoryAdvisor, successfulSessionRequestAdvisor)
                .build()

        stream(client, sessionId, userId, "My project codename is Aurora")
        stream(client, sessionId, userId, "Acknowledge the second turn")
        stream(client, sessionId, userId, "What was the codename?")

        val thirdPrompt = model.prompts[2]
        assertTrue(
            thirdPrompt.instructions
                .filterIsInstance<UserMessage>()
                .any { it.text == "My project codename is Aurora" },
        )
        assertEquals(6, sessionService.getEvents(sessionId.toString()).size)
    }

    @Test
    fun `tool result is recalled without calling the tool again`() {
        val model = ToolRecallModel()
        val sessionId = UUID.randomUUID()
        val userId = TestAuditorConfig.TEST_USER_ID
        val client =
            ChatClient
                .builder(model)
                .defaultAdvisors(
                    sessionMemoryAdvisor,
                    successfulSessionRequestAdvisor,
                    productionToolCallingAdvisor(),
                ).build()
        val tools = ToolCallbacks.from(SupportPolicyTool())

        val first =
            stream(
                client,
                sessionId,
                userId,
                "Look up the PREMIUM P1 response target",
                tools = tools,
            )
        val second =
            stream(
                client,
                sessionId,
                userId,
                "What response target did the tool return?",
                tools = tools,
            )

        assertEquals("The response target is 15 minutes.", first)
        assertEquals("It returned 15 minutes.", second)
        assertEquals(1, model.requestedToolCalls)
        val messages = sessionService.getMessages(sessionId.toString())
        assertTrue(messages.filterIsInstance<AssistantMessage>().any { it.hasToolCalls() })
        assertTrue(messages.filterIsInstance<ToolResponseMessage>().isNotEmpty())
    }

    @Test
    fun `legacy conversation is backfilled on first reopen`() {
        val user = userRepository.findById(TestAuditorConfig.TEST_USER_ID).orElseThrow()
        val conversation =
            conversationRepository.save(
                Conversation(title = "Legacy conversation").also { it.createdBy = user },
            )
        messageRepository.save(
            ChatMessage(
                content = "The deployment color is blue",
                conversation = conversation,
                type = ChatMessageType.QUESTION,
            ).also { it.createdBy = user },
        )
        messageRepository.save(
            ChatMessage(
                content = "I will remember blue",
                conversation = conversation,
                type = ChatMessageType.ANSWER,
            ).also { it.createdBy = user },
        )

        val sessionId =
            conversationSessionService.resolveSessionId(
                conversationId = requireNotNull(conversation.id),
                userId = TestAuditorConfig.TEST_USER_ID,
                agentId = UUID.randomUUID(),
                correlationId = "backfill-test",
            )

        val reloaded = conversationRepository.findById(conversation.id!!).orElseThrow()
        assertEquals(sessionId, reloaded.sessionId)
        assertEquals(
            listOf("The deployment color is blue", "I will remember blue"),
            sessionService.getMessages(sessionId.toString()).map { it.text },
        )
    }

    @Test
    fun `another user cannot resolve the session of an existing conversation`() {
        val owner = userRepository.findById(TestAuditorConfig.TEST_USER_ID).orElseThrow()
        val conversation =
            conversationRepository.save(
                Conversation(title = "Owned conversation", sessionId = UUID.randomUUID())
                    .also { it.createdBy = owner },
            )

        // A conversation that already carries a session must not short-circuit past the
        // ownership check, and must not reveal that it exists.
        assertThrows<ResourceNotFoundException> {
            conversationSessionService.resolveSessionId(
                conversationId = requireNotNull(conversation.id),
                userId = UUID.randomUUID(),
                agentId = UUID.randomUUID(),
                correlationId = "cross-user",
            )
        }
    }

    @Test
    fun `long session context is included in the preflight budget check`() {
        val userId = TestAuditorConfig.TEST_USER_ID
        val agentId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        sessionService.create(
            CreateSessionRequest
                .builder()
                .id(sessionId.toString())
                .userId(userId.toString())
                .build(),
        )
        sessionService.appendMessage(sessionId.toString(), UserMessage("history ".repeat(2_000)))

        val chatStreamService = mockk<ChatStreamService>()
        val dynamicChatModelService = mockk<DynamicChatModelService>()
        val tokenFacade = mockk<TokenAccountingFacade>()
        val capturedHistory = slot<List<String>>()
        every { dynamicChatModelService.getAgentConfig(agentId) } returns
            mockk<AgentResponseDto> { every { model } returns "gpt-4o-mini" }
        every { tokenFacade.estimateInput("gpt-4o-mini", any(), capture(capturedHistory)) } returns 10_000
        every {
            tokenFacade.assertInputBudget(userId, TokenOperation.CHAT, 10_000)
        } throws TokenLimitExceededException("budget exceeded")

        val service =
            ChatModelService(
                chatStreamService = chatStreamService,
                summarizationService = mockk<SummarizationService>(),
                dynamicChatModelService = dynamicChatModelService,
                tokenFacade = tokenFacade,
                sessionService = sessionService,
            )

        assertThrows<TokenLimitExceededException> {
            service.call(
                userId = userId,
                sessionId = sessionId,
                request = request(agentId, "continue"),
            )
        }
        assertTrue(capturedHistory.captured.single().length > 10_000)
        verify(exactly = 0) { chatStreamService.stream(any(), any(), any(), any()) }
    }

    @Test
    fun `session rejects a different user`() {
        val sessionId = UUID.randomUUID()
        val owner = TestAuditorConfig.TEST_USER_ID
        val otherUser = UUID.randomUUID()
        val client =
            ChatClient
                .builder(CapturingModel())
                .defaultAdvisors(sessionMemoryAdvisor, successfulSessionRequestAdvisor)
                .build()

        stream(client, sessionId, owner, "private context")

        val failure =
            assertThrows<IllegalStateException> {
                stream(client, sessionId, otherUser, "show me the context")
            }
        assertTrue(failure.message.orEmpty().contains("Access denied"))
    }

    @Test
    fun `stream appends one event per role and a failed turn appends none`() {
        val userId = TestAuditorConfig.TEST_USER_ID
        val agentId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val toolModel = ToolRecallModel()
        val chatClient =
            ChatClient
                .builder(toolModel)
                .defaultAdvisors(productionToolCallingAdvisor())
                .build()
        val chatClientFactory = mockk<AgentChatClientFactory>()
        val advisorRegistry = mockk<CallAdvisorRegistry>()
        val toolFacade = mockk<ToolExecutionFacade>()
        val tokenFacade = mockk<TokenAccountingFacade>(relaxed = true)
        every { chatClientFactory.create(agentId) } returns chatClient
        every { advisorRegistry.resolveForAgent(agentId, userId, any()) } returns
            listOf(sessionMemoryAdvisor, successfulSessionRequestAdvisor)
        every { toolFacade.createToolCallbacks(userId, agentId, any()) } returns
            ToolCallbacks.from(SupportPolicyTool()).toList()
        val chatStreamService =
            ChatStreamService(
                toolFacade = toolFacade,
                chatClientFactory = chatClientFactory,
                callAdvisorRegistry = advisorRegistry,
                tokenFacade = tokenFacade,
                reasoningProperties = ChatReasoningProperties(enabled = true),
            )
        val dynamicChatModelService = mockk<DynamicChatModelService>()
        every { dynamicChatModelService.getAgentConfig(agentId) } returns
            mockk<AgentResponseDto> { every { model } returns "test-model" }
        every { tokenFacade.estimateInput("test-model", any(), any()) } returns 2
        val chatModelService =
            ChatModelService(
                chatStreamService = chatStreamService,
                summarizationService = mockk<SummarizationService>(),
                dynamicChatModelService = dynamicChatModelService,
                tokenFacade = tokenFacade,
                sessionService = sessionService,
            )
        val commandService = mockk<ConversationCommandService>()
        every { commandService.createConversationWithFirstMessage(any(), any(), any(), any()) } returns
            mockk<ChatResponseDto>()
        val conversationSessionService = mockk<ConversationSessionService>()
        every { conversationSessionService.resolveSessionId(any(), any(), any(), any()) } returns sessionId
        val service =
            ConversationStreamingService(
                chatModelService = chatModelService,
                commandService = commandService,
                conversationSessionService = conversationSessionService,
            )

        val successEvents =
            service
                .streamConversation(request(agentId, "single turn"), userId)
                .collectList()
                .block()
                .orEmpty()

        val persistedEvents = sessionService.getEvents(sessionId.toString())
        assertEquals(4, persistedEvents.size)
        assertEquals(4, persistedEvents.map { it.id }.distinct().size)

        every { chatClientFactory.create(agentId) } returns
            ChatClient.builder(FailingModel()).build()
        val errorEvents =
            service
                .streamConversation(request(agentId, "failing turn"), userId)
                .collectList()
                .block()
                .orEmpty()

        assertEquals(persistedEvents.size, sessionService.getEvents(sessionId.toString()).size)

        assertEquals(
            setOf("message", "tool", "reasoning", "complete", "error"),
            (successEvents + errorEvents).mapNotNull { it.event() }.toSet(),
        )
        verify(exactly = 1, timeout = 2_000) { tokenFacade.recordWithFallback(any(), any()) }
    }

    private fun stream(
        client: ChatClient,
        sessionId: UUID,
        userId: UUID,
        text: String,
        tools: Array<org.springframework.ai.tool.ToolCallback> = emptyArray(),
    ): String =
        client
            .prompt()
            .advisors { spec ->
                spec
                    .param(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, sessionId.toString())
                    .param(SessionMemoryAdvisor.USER_ID_CONTEXT_KEY, userId.toString())
                    .param(SuccessfulSessionRequestAdvisor.RUN_ID_CONTEXT_KEY, UUID.randomUUID().toString())
            }.tools(*tools)
            .user(text)
            .stream()
            .content()
            .collectList()
            .block()
            .orEmpty()
            .joinToString("")

    private fun request(
        agentId: UUID,
        question: String,
    ) = ChatRequestDto(
        question = question,
        conversationId = null,
        files = null,
        agentId = agentId,
    )

    private fun productionToolCallingAdvisor(): ToolCallingAdvisor =
        ToolCallingAdvisor
            .builder()
            .advisorOrder(ToolCallingConfig.TOOL_CALLING_ADVISOR_ORDER)
            .build()

    private class CapturingModel : ChatModel {
        val prompts = mutableListOf<Prompt>()

        override fun call(prompt: Prompt): ChatResponse = response(prompt)

        override fun stream(prompt: Prompt): Flux<ChatResponse> = Flux.just(response(prompt))

        override fun getOptions(): ChatOptions = ToolCallingChatOptions.builder().build()

        private fun response(prompt: Prompt): ChatResponse {
            prompts.add(prompt)
            return ChatResponse(listOf(Generation(AssistantMessage("acknowledged"))))
        }
    }

    private class ToolRecallModel : ChatModel {
        var requestedToolCalls = 0

        override fun call(prompt: Prompt): ChatResponse = response(prompt)

        override fun stream(prompt: Prompt): Flux<ChatResponse> = Flux.just(response(prompt))

        override fun getOptions(): ChatOptions = ToolCallingChatOptions.builder().build()

        private fun response(prompt: Prompt): ChatResponse {
            val currentQuestion =
                prompt.instructions
                    .filterIsInstance<UserMessage>()
                    .last()
                    .text
                    .orEmpty()
            val toolResponse = prompt.instructions.filterIsInstance<ToolResponseMessage>().lastOrNull()

            if (toolResponse == null) {
                requestedToolCalls++
                val toolCall =
                    AssistantMessage.ToolCall(
                        "session-tool-call-1",
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
                                .properties(mapOf("reasoningContent" to "Looking up the support policy."))
                                .toolCalls(listOf(toolCall))
                                .build(),
                            ChatGenerationMetadata.builder().finishReason("tool_calls").build(),
                        ),
                    ),
                )
            }

            val answer =
                if (currentQuestion.startsWith("What response")) {
                    "It returned 15 minutes."
                } else {
                    "The response target is 15 minutes."
                }
            return ChatResponse(
                listOf(
                    Generation(
                        AssistantMessage(answer),
                        ChatGenerationMetadata
                            .builder()
                            .finishReason("stop")
                            .metadata("thinking", "Using the stored tool result.")
                            .build(),
                    ),
                ),
            )
        }
    }

    private class FailingModel : ChatModel {
        override fun call(prompt: Prompt): ChatResponse = error("provider unavailable")

        override fun stream(prompt: Prompt): Flux<ChatResponse> =
            Flux.error(IllegalStateException("provider unavailable"))

        override fun getOptions(): ChatOptions = ToolCallingChatOptions.builder().build()
    }
}
