package com.ntgjvmagent.orchestrator.integration.session

import com.ninjasquad.springmockk.MockkBean
import com.ntgjvmagent.orchestrator.advisor.SessionMemoryAdvisorFactory
import com.ntgjvmagent.orchestrator.advisor.SuccessfulSessionRequestAdvisor
import com.ntgjvmagent.orchestrator.component.AgentChatClientFactory
import com.ntgjvmagent.orchestrator.dto.ChatRequestDto
import com.ntgjvmagent.orchestrator.dto.response.AgentResponseDto
import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.exception.TokenLimitExceededException
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.integration.config.TestAuditorConfig
import com.ntgjvmagent.orchestrator.model.ProviderType
import com.ntgjvmagent.orchestrator.model.TokenOperation
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import com.ntgjvmagent.orchestrator.repository.TokenUsageLogRepository
import com.ntgjvmagent.orchestrator.service.ChatModelService
import com.ntgjvmagent.orchestrator.service.ChatStreamService
import com.ntgjvmagent.orchestrator.service.DynamicChatModelService
import com.ntgjvmagent.orchestrator.service.SummarizationService
import com.ntgjvmagent.orchestrator.token.accounting.TokenAccountingFacade
import com.ntgjvmagent.orchestrator.token.cache.DailyTokenUsageCache
import com.ntgjvmagent.orchestrator.token.estimation.SpringAiTokenCountEstimatorAdapter
import com.ntgjvmagent.orchestrator.token.estimation.TokenEstimatorSelector
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.metadata.ChatResponseMetadata
import org.springframework.ai.chat.metadata.DefaultUsage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.content.MediaContent
import org.springframework.ai.model.tool.ToolCallingChatOptions
import org.springframework.ai.session.CreateSessionRequest
import org.springframework.ai.session.SessionService
import org.springframework.ai.session.advisor.SessionMemoryAdvisor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import reactor.core.publisher.Flux
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

@TestPropertySource(
    properties = [
        "session.compaction.enabled=true",
        "session.compaction.turn-threshold=2",
        "session.compaction.token-threshold=1000000",
        "session.compaction.max-events-to-keep=2",
        "session.compaction.overlap-size=1",
    ],
)
class SessionCompactionIT : BaseIntegrationTest() {
    @Autowired
    private lateinit var sessionService: SessionService

    @Autowired
    private lateinit var advisorFactory: SessionMemoryAdvisorFactory

    @Autowired
    private lateinit var successfulSessionRequestAdvisor: SuccessfulSessionRequestAdvisor

    @Autowired
    private lateinit var tokenUsageLogRepository: TokenUsageLogRepository

    @Autowired
    private lateinit var agentRepository: AgentRepository

    @Autowired
    private lateinit var estimatorSelector: TokenEstimatorSelector

    @MockkBean
    private lateinit var chatClientFactory: AgentChatClientFactory

    @MockkBean
    private lateinit var dynamicChatModelService: DynamicChatModelService

    @MockkBean(relaxUnitFun = true)
    private lateinit var dailyTokenUsageCache: DailyTokenUsageCache

    private val userId = TestAuditorConfig.TEST_USER_ID
    private lateinit var agentId: UUID

    @BeforeEach
    fun configureAgent() {
        tokenUsageLogRepository.deleteAll()
        agentId =
            requireNotNull(
                agentRepository
                    .save(
                        Agent(
                            name = "compaction-test-${UUID.randomUUID()}",
                            provider = ProviderType.OPENAI,
                            baseUrl = "https://example.test",
                            apiKey = "test",
                            chatCompletionsPath = "/chat/completions",
                            model = "gpt-4o-mini",
                        ),
                    ).id,
            )
        every { dynamicChatModelService.getAgentConfig(agentId) } returns agentConfig()
        every { dailyTokenUsageCache.getUsedTokens(userId, any()) } returns 0L
    }

    @Test
    fun `long conversation compacts coherently and records summarization usage`() {
        val sessionId = createSession()
        val compactionModel = CompactionModel()
        val conversationModel = CoherentConversationModel()
        every { chatClientFactory.create(agentId) } returns ChatClient.builder(compactionModel).build()
        val advisor = advisorFactory.create(agentId, userId, ROOT_CORRELATION_ID)
        val client =
            ChatClient
                .builder(conversationModel)
                .defaultAdvisors(advisor, successfulSessionRequestAdvisor)
                .build()
        val estimator = SpringAiTokenCountEstimatorAdapter("gpt-4o-mini", estimatorSelector)

        stream(client, sessionId, "My project codename is Aurora. ${"alpha ".repeat(600)}")
        stream(client, sessionId, "Keep this background in mind. ${"beta ".repeat(600)}")
        stream(client, sessionId, "One more detail for the project. ${"gamma ".repeat(600)}")
        val tokensBefore = estimator.estimate(conversationModel.prompts[2].mediaContents())

        awaitCompaction(sessionId)
        val answer = stream(client, sessionId, "What is the project codename?")
        val tokensAfter = estimator.estimate(conversationModel.prompts[3].mediaContents())

        val accountingRows =
            tokenUsageLogRepository.findAll().filter {
                it.operation == TokenOperation.SUMMARIZATION &&
                    it.correlationId == "$ROOT_CORRELATION_ID:compaction"
            }
        assertEquals("The project codename is Aurora.", answer)
        assertEquals(1, compactionModel.calls)
        assertEquals(1, accountingRows.size)
        assertEquals(132L, accountingRows.single().totalTokens)
        assertEquals(1_829, tokensBefore)
        assertEquals(635, tokensAfter)
        assertTrue(tokensAfter < tokensBefore, "Expected $tokensAfter to be lower than $tokensBefore")
    }

    @Test
    fun `budget check still blocks after session context is compacted`() {
        val sessionId = createSession()
        val compactionModel = CompactionModel()
        every { chatClientFactory.create(agentId) } returns ChatClient.builder(compactionModel).build()
        val advisor = advisorFactory.create(agentId, userId, "chat-budget")
        val client =
            ChatClient
                .builder(CoherentConversationModel())
                .defaultAdvisors(advisor, successfulSessionRequestAdvisor)
                .build()

        stream(client, sessionId, "My project codename is Aurora. ${"alpha ".repeat(300)}")
        stream(client, sessionId, "Remember the project context. ${"beta ".repeat(300)}")
        stream(client, sessionId, "Preserve the important facts. ${"gamma ".repeat(300)}")
        awaitCompaction(sessionId)

        val tokenFacade = mockk<TokenAccountingFacade>()
        val history = slot<List<String>>()
        every { tokenFacade.estimateInput("gpt-4o-mini", any(), capture(history)) } returns 500
        every {
            tokenFacade.assertInputBudget(userId, TokenOperation.CHAT, 500)
        } throws TokenLimitExceededException("budget exceeded")
        val streamService = mockk<ChatStreamService>()
        val service =
            ChatModelService(
                chatStreamService = streamService,
                summarizationService = mockk<SummarizationService>(),
                dynamicChatModelService = dynamicChatModelService,
                tokenFacade = tokenFacade,
                sessionService = sessionService,
            )

        assertThrows<TokenLimitExceededException> {
            service.call(userId, sessionId, request("continue"))
        }
        assertTrue(history.captured.any { it.contains("Aurora") })
        verify(exactly = 0) { streamService.stream(any(), any(), any(), any()) }
    }

    @Test
    fun `a failing compaction does not break the chat stream`() {
        val sessionId = createSession()
        every { chatClientFactory.create(agentId) } returns
            ChatClient.builder(FailingCompactionModel()).build()
        val advisor = advisorFactory.create(agentId, userId, "chat-failing-compaction")
        val client =
            ChatClient
                .builder(CoherentConversationModel())
                .defaultAdvisors(advisor, successfulSessionRequestAdvisor)
                .build()

        // turn-threshold is 2, so the second turn already asks for compaction and its
        // summarization call blows up inside SessionMemoryAdvisor.after().
        stream(client, sessionId, "My project codename is Aurora.")
        stream(client, sessionId, "Keep this background in mind.")
        val answer = stream(client, sessionId, "What is the project codename?")

        assertEquals("The project codename is Aurora.", answer)
        assertTrue(
            sessionService.getEvents(sessionId.toString()).none { it.isSynthetic },
            "Failed compaction must leave the event log unchanged",
        )
    }

    private fun createSession(): UUID {
        val sessionId = UUID.randomUUID()
        sessionService.create(
            CreateSessionRequest
                .builder()
                .id(sessionId.toString())
                .userId(userId.toString())
                .metadata("agentId", agentId.toString())
                .build(),
        )
        return sessionId
    }

    private fun stream(
        client: ChatClient,
        sessionId: UUID,
        text: String,
    ): String =
        client
            .prompt()
            .advisors { spec ->
                spec
                    .param(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, sessionId.toString())
                    .param(SessionMemoryAdvisor.USER_ID_CONTEXT_KEY, userId.toString())
                    .param(SuccessfulSessionRequestAdvisor.RUN_ID_CONTEXT_KEY, UUID.randomUUID().toString())
            }.user(text)
            .stream()
            .content()
            .collectList()
            .block()
            .orEmpty()
            .joinToString("")

    private fun awaitCompaction(sessionId: UUID) {
        repeat(100) {
            if (sessionService.getEvents(sessionId.toString()).any { it.isSynthetic }) return
            Thread.sleep(20)
        }
        error("Session did not compact")
    }

    private fun request(question: String) =
        ChatRequestDto(
            question = question,
            conversationId = null,
            files = null,
            agentId = agentId,
            correlationId = "chat-budget-next",
        )

    private fun agentConfig() =
        AgentResponseDto(
            id = agentId,
            name = "compaction-test",
            description = null,
            avatar = null,
            active = true,
            provider = ProviderType.OPENAI,
            baseUrl = "https://example.test",
            apiKey = "test",
            chatCompletionsPath = "/chat/completions",
            model = "gpt-4o-mini",
            topP = 1.0,
            temperature = 0.0,
            maxTokens = 2_048,
            frequencyPenalty = 0.0,
            presencePenalty = 0.0,
            settings = null,
        )

    private fun Prompt.mediaContents(): List<MediaContent> = instructions.filterIsInstance<MediaContent>()

    private class CompactionModel : ChatModel {
        var calls = 0

        override fun call(prompt: Prompt): ChatResponse {
            calls++
            return ChatResponse
                .builder()
                .generations(listOf(Generation(AssistantMessage("The project codename is Aurora."))))
                .metadata(
                    ChatResponseMetadata
                        .builder()
                        .usage(DefaultUsage(120, 12, 132))
                        .build(),
                ).build()
        }

        override fun stream(prompt: Prompt): Flux<ChatResponse> = Flux.just(call(prompt))

        override fun getOptions(): ChatOptions = ToolCallingChatOptions.builder().build()
    }

    private class FailingCompactionModel : ChatModel {
        override fun call(prompt: Prompt): ChatResponse = throw IllegalStateException("summarization provider down")

        override fun stream(prompt: Prompt): Flux<ChatResponse> =
            Flux.error(IllegalStateException("summarization provider down"))

        override fun getOptions(): ChatOptions = ToolCallingChatOptions.builder().build()
    }

    private class CoherentConversationModel : ChatModel {
        val prompts = CopyOnWriteArrayList<Prompt>()

        override fun call(prompt: Prompt): ChatResponse = response(prompt)

        override fun stream(prompt: Prompt): Flux<ChatResponse> = Flux.just(response(prompt))

        override fun getOptions(): ChatOptions = ToolCallingChatOptions.builder().build()

        private fun response(prompt: Prompt): ChatResponse {
            prompts.add(prompt)
            val question =
                prompt.instructions
                    .filterIsInstance<UserMessage>()
                    .last()
                    .text
                    .orEmpty()
            val remembersAurora = prompt.instructions.any { it.text.orEmpty().contains("Aurora") }
            val content =
                if (question == "What is the project codename?" && remembersAurora) {
                    "The project codename is Aurora."
                } else {
                    "Acknowledged."
                }
            return ChatResponse(listOf(Generation(AssistantMessage(content))))
        }
    }

    companion object {
        private const val ROOT_CORRELATION_ID = "chat-compaction-it"
    }
}
