package com.ntgjvmagent.orchestrator.integration.question

import com.ntgjvmagent.orchestrator.config.ToolCallingConfig
import com.ntgjvmagent.orchestrator.dto.UserQuestionDto
import com.ntgjvmagent.orchestrator.dto.UserQuestionOptionDto
import com.ntgjvmagent.orchestrator.entity.Conversation
import com.ntgjvmagent.orchestrator.entity.PendingQuestion
import com.ntgjvmagent.orchestrator.exception.BadRequestException
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.integration.config.TestAuditorConfig
import com.ntgjvmagent.orchestrator.model.PendingQuestionStatus
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import com.ntgjvmagent.orchestrator.repository.ConversationRepository
import com.ntgjvmagent.orchestrator.repository.PendingQuestionRepository
import com.ntgjvmagent.orchestrator.service.PendingQuestionService
import com.ntgjvmagent.orchestrator.tool.AskUserQuestionContext
import com.ntgjvmagent.orchestrator.tool.LocalToolCatalog
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor
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
import org.springframework.ai.session.SessionService
import org.springframework.ai.session.advisor.SessionMemoryAdvisor
import org.springframework.ai.tool.toolsearch.index.regex.RegexToolIndex
import org.springframework.beans.factory.annotation.Autowired
import reactor.core.publisher.Flux
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PendingQuestionIT : BaseIntegrationTest() {
    @Autowired
    private lateinit var pendingQuestionService: PendingQuestionService

    @Autowired
    private lateinit var pendingQuestionRepository: PendingQuestionRepository

    @Autowired
    private lateinit var conversationRepository: ConversationRepository

    @Autowired
    private lateinit var agentRepository: AgentRepository

    @Autowired
    private lateinit var sessionService: SessionService

    @Autowired
    private lateinit var localToolCatalog: LocalToolCatalog

    private lateinit var scheduler: Scheduler
    private lateinit var sessionAdvisor: SessionMemoryAdvisor

    @BeforeEach
    fun configureAdvisor() {
        scheduler = Schedulers.newBoundedElastic(2, 20, "pending-question-session")
        sessionAdvisor =
            SessionMemoryAdvisor
                .builder(sessionService)
                .scheduler(scheduler)
                .build()
    }

    @AfterEach
    fun disposeAdvisor() {
        scheduler.dispose()
        pendingQuestionRepository.deleteAll()
    }

    @Test
    fun `question answer resumes the same conversation and session`() {
        val userId = TestAuditorConfig.TEST_USER_ID
        val agentId = requireNotNull(agentRepository.findAll().first().id)
        val sessionId = UUID.randomUUID()
        sessionService.create(
            CreateSessionRequest
                .builder()
                .id(sessionId.toString())
                .userId(userId.toString())
                .build(),
        )
        val emitted = mutableListOf<com.ntgjvmagent.orchestrator.dto.PendingQuestionDto>()
        val tools =
            localToolCatalog.getToolCallbacks(
                agentId = agentId,
                questionContext =
                    AskUserQuestionContext(
                        userId = userId,
                        agentId = agentId,
                        sessionId = sessionId,
                        conversationId = null,
                        correlationId = "question-it",
                    ),
                onQuestion = emitted::add,
            )
        assertTrue(
            tools.any { it.toolDefinition.name() == LocalToolCatalog.ASK_USER_QUESTION_TOOL_NAME },
            "Registered tools: ${tools.map { it.toolDefinition.name() }}",
        )
        val model = ClarificationModel()
        val client =
            ChatClient
                .builder(model)
                .defaultAdvisors(
                    sessionAdvisor,
                    ToolSearchToolCallingAdvisor
                        .builder()
                        .toolIndex(RegexToolIndex())
                        .conversationHistoryEnabled(false)
                        .advisorOrder(ToolCallingConfig.TOOL_CALLING_ADVISOR_ORDER)
                        .build(),
                ).build()

        val first = stream(client, sessionId, userId, "Deploy the account change", tools.toTypedArray())
        assertEquals("Waiting for the user's answer.", first)
        val pending = emitted.single()

        val owner = userRepository.findById(userId).orElseThrow()
        val conversation =
            conversationRepository.save(
                Conversation(title = "Account change", sessionId = sessionId).also { it.createdBy = owner },
            )
        pendingQuestionService.attachConversation(pending.id, requireNotNull(conversation.id), userId)
        val resolution =
            pendingQuestionService.beginResolution(
                pending.id,
                requireNotNull(conversation.id),
                userId,
                mapOf("Which environment should receive the change?" to "Production"),
            )

        val resumed = stream(client, resolution.sessionId, userId, resolution.prompt)
        pendingQuestionService.completeResolution(pending.id, userId)

        assertEquals("Continuing the original request in Production.", resumed)
        assertEquals(sessionId, resolution.sessionId)
        assertTrue(model.resumeSawOriginalRequest)
        assertTrue(
            sessionService.getMessages(sessionId.toString()).filterIsInstance<UserMessage>().any {
                it.text.orEmpty().contains("Production")
            },
        )
    }

    @Test
    fun `duplicate answer is rejected and expired pending questions are cleaned`() {
        val userId = TestAuditorConfig.TEST_USER_ID
        val agentId = requireNotNull(agentRepository.findAll().first().id)
        val sessionId = UUID.randomUUID()
        val owner = userRepository.findById(userId).orElseThrow()
        val conversation =
            conversationRepository.save(
                Conversation(title = "Pending lifecycle", sessionId = sessionId).also { it.createdBy = owner },
            )
        val question = question()
        val pending =
            pendingQuestionService.create(
                userId,
                agentId,
                sessionId,
                requireNotNull(conversation.id),
                "lifecycle-it",
                listOf(question),
            )

        val answer = mapOf(question.question to "Production")
        pendingQuestionService.beginResolution(pending.id, requireNotNull(conversation.id), userId, answer)
        pendingQuestionService.completeResolution(pending.id, userId)
        assertThrows<BadRequestException> {
            pendingQuestionService.beginResolution(pending.id, requireNotNull(conversation.id), userId, answer)
        }

        val expired =
            pendingQuestionRepository.save(
                PendingQuestion(
                    sessionId = UUID.randomUUID(),
                    conversationId = requireNotNull(conversation.id),
                    userId = userId,
                    agentId = agentId,
                    correlationId = "expired-it",
                    questionsJson = objectMapper.writeValueAsString(listOf(question)),
                    status = PendingQuestionStatus.ANSWERING,
                    expiresAt = Instant.now().minusSeconds(1),
                ),
            )

        assertEquals(1, pendingQuestionService.expireDue())
        assertEquals(
            PendingQuestionStatus.EXPIRED,
            pendingQuestionRepository.findById(requireNotNull(expired.id)).orElseThrow().status,
        )
        assertNotNull(conversationRepository.findById(requireNotNull(conversation.id)).orElse(null))
    }

    @Test
    fun `many persisted questions complete without exhausting a small worker pool`() {
        val userId = TestAuditorConfig.TEST_USER_ID
        val agentId = requireNotNull(agentRepository.findAll().first().id)
        val executor = Executors.newFixedThreadPool(4)

        try {
            val futures =
                (1..LOAD_QUESTION_COUNT).map { index ->
                    executor.submit<String> {
                        val callback =
                            localToolCatalog
                                .getToolCallbacks(
                                    agentId = agentId,
                                    questionContext =
                                        AskUserQuestionContext(
                                            userId = userId,
                                            agentId = agentId,
                                            sessionId = UUID.randomUUID(),
                                            conversationId = null,
                                            correlationId = "load-$index",
                                        ),
                                ).single {
                                    it.toolDefinition.name() == LocalToolCatalog.ASK_USER_QUESTION_TOOL_NAME
                                }
                        callback.call(QUESTION_ARGUMENTS)
                    }
                }

            executor.shutdown()
            assertTrue(executor.awaitTermination(LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS))
            assertTrue(futures.all { it.isDone })
            assertEquals(LOAD_QUESTION_COUNT.toLong(), pendingQuestionRepository.count())
        } finally {
            executor.shutdownNow()
        }
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
            }.tools(*tools)
            .user(text)
            .stream()
            .content()
            .collectList()
            .block()
            .orEmpty()
            .joinToString("")

    private fun question() =
        UserQuestionDto(
            question = "Which environment should receive the change?",
            header = "Environment",
            options =
                listOf(
                    UserQuestionOptionDto("Staging", "Use the staging account"),
                    UserQuestionOptionDto("Production", "Use the production account"),
                ),
            multiSelect = false,
        )

    private class ClarificationModel : ChatModel {
        var resumeSawOriginalRequest = false

        override fun call(prompt: Prompt): ChatResponse = response(prompt)

        override fun stream(prompt: Prompt): Flux<ChatResponse> = Flux.just(response(prompt))

        override fun getOptions(): ChatOptions = ToolCallingChatOptions.builder().build()

        private fun response(prompt: Prompt): ChatResponse {
            val users = prompt.instructions.filterIsInstance<UserMessage>()
            val current = users.lastOrNull()?.text.orEmpty()
            if (current.startsWith("The user answered")) {
                resumeSawOriginalRequest = users.any { it.text == "Deploy the account change" }
                return finalResponse("Continuing the original request in Production.")
            }
            val lastToolName =
                prompt.instructions
                    .filterIsInstance<ToolResponseMessage>()
                    .flatMap { it.responses }
                    .lastOrNull()
                    ?.name()
            if (lastToolName == LocalToolCatalog.ASK_USER_QUESTION_TOOL_NAME) {
                return finalResponse("Waiting for the user's answer.")
            }

            val call =
                if (lastToolName == "toolSearchTool") {
                    AssistantMessage.ToolCall(
                        "ask-user-question-it",
                        "function",
                        LocalToolCatalog.ASK_USER_QUESTION_TOOL_NAME,
                        """{"questions":[{"question":"Which environment should receive the change?","header":"Environment","options":[{"label":"Staging","description":"Use the staging account"},{"label":"Production","description":"Use the production account"}],"multiSelect":false}]}""",
                    )
                } else {
                    AssistantMessage.ToolCall(
                        "search-question-tool-it",
                        "function",
                        "toolSearchTool",
                        """{"query":"AskUserQuestionTool clarification","maxResults":2}""",
                    )
                }
            return ChatResponse(
                listOf(
                    Generation(
                        AssistantMessage
                            .builder()
                            .content("")
                            .toolCalls(listOf(call))
                            .build(),
                        ChatGenerationMetadata.builder().finishReason("tool_calls").build(),
                    ),
                ),
            )
        }

        private fun finalResponse(text: String) =
            ChatResponse(
                listOf(
                    Generation(
                        AssistantMessage(text),
                        ChatGenerationMetadata.builder().finishReason("stop").build(),
                    ),
                ),
            )
    }

    private companion object {
        const val LOAD_QUESTION_COUNT = 200
        const val LOAD_TIMEOUT_SECONDS = 15L
        const val QUESTION_ARGUMENTS =
            """{"questions":[{"question":"Which environment should receive the change?","header":"Environment","options":[{"label":"Staging","description":"Use the staging account"},{"label":"Production","description":"Use the production account"}],"multiSelect":false}]}"""
    }
}
