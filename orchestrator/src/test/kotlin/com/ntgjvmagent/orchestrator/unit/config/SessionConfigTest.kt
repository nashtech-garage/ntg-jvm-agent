package com.ntgjvmagent.orchestrator.unit.config

import com.ntgjvmagent.orchestrator.advisor.FailSafeCompactionStrategy
import com.ntgjvmagent.orchestrator.advisor.FailSafeCompactionTrigger
import com.ntgjvmagent.orchestrator.component.AgentChatClientFactory
import com.ntgjvmagent.orchestrator.config.SessionCompactionProperties
import com.ntgjvmagent.orchestrator.config.SessionConfig
import com.ntgjvmagent.orchestrator.config.ToolCallingConfig
import com.ntgjvmagent.orchestrator.dto.response.AgentResponseDto
import com.ntgjvmagent.orchestrator.model.ProviderType
import com.ntgjvmagent.orchestrator.service.DynamicChatModelService
import com.ntgjvmagent.orchestrator.token.accounting.TokenAccountingFacade
import com.ntgjvmagent.orchestrator.token.estimation.GptTokenEstimator
import com.ntgjvmagent.orchestrator.token.estimation.HeuristicTokenEstimator
import com.ntgjvmagent.orchestrator.token.estimation.TokenEstimatorSelector
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.session.SessionService
import org.springframework.ai.session.compaction.TokenCountTrigger
import reactor.core.scheduler.Schedulers
import java.util.UUID

class SessionConfigTest {
    @Test
    fun `factory configures both triggers and strategy with the same estimator`() {
        val agentId = UUID.randomUUID()
        val chatClientFactory = mockk<AgentChatClientFactory>()
        val dynamicChatModelService = mockk<DynamicChatModelService>()
        every { dynamicChatModelService.getAgentConfig(agentId) } returns agent(agentId)
        every { chatClientFactory.createWithoutToolSearch(agentId) } returns
            ChatClient.builder(mockk<ChatModel>(relaxed = true)).build()
        val factory =
            sessionMemoryAdvisorFactory(
                chatClientFactory = chatClientFactory,
                dynamicChatModelService = dynamicChatModelService,
                tokenFacade = mockk(relaxed = true),
            )

        val advisor = factory.create(agentId, UUID.randomUUID(), "chat-root")
        // Both are wrapped so a compaction failure cannot abort the chat request; unwrap to
        // inspect the library objects underneath.
        val trigger = (field(advisor, "compactionTrigger") as FailSafeCompactionTrigger).delegate
        val strategy = (field(advisor, "compactionStrategy") as FailSafeCompactionStrategy).delegate
        val triggers = field(trigger, "triggers") as List<*>
        val tokenTrigger = triggers.filterIsInstance<TokenCountTrigger>().single()

        assertEquals(ToolCallingConfig.TOOL_CALLING_ADVISOR_ORDER + 1, advisor.order)
        assertNotNull(strategy)
        assertSame(field(tokenTrigger, "tokenCountEstimator"), field(strategy, "tokenCountEstimator"))
    }

    @Test
    fun `agent settings can disable compaction`() {
        val agentId = UUID.randomUUID()
        val chatClientFactory = mockk<AgentChatClientFactory>()
        val dynamicChatModelService = mockk<DynamicChatModelService>()
        every { dynamicChatModelService.getAgentConfig(agentId) } returns
            agent(agentId, mapOf("sessionCompaction" to mapOf("enabled" to false)))
        val factory =
            sessionMemoryAdvisorFactory(
                chatClientFactory = chatClientFactory,
                dynamicChatModelService = dynamicChatModelService,
                tokenFacade = mockk(),
            )

        val advisor = factory.create(agentId, UUID.randomUUID(), "chat-root")

        assertNull(field(advisor, "compactionTrigger"))
        assertNull(field(advisor, "compactionStrategy"))
        verify(exactly = 0) { chatClientFactory.createWithoutToolSearch(any()) }
    }

    @Test
    fun `token trigger without explicit estimator reproduces library defect`() {
        assertThrows<IllegalArgumentException> {
            TokenCountTrigger.builder().threshold(4_000).build()
        }
    }

    private fun agent(
        id: UUID,
        settings: Map<String, Any>? = null,
    ) = AgentResponseDto(
        id = id,
        name = "test",
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
        settings = settings,
    )

    private fun sessionMemoryAdvisorFactory(
        chatClientFactory: AgentChatClientFactory,
        dynamicChatModelService: DynamicChatModelService,
        tokenFacade: TokenAccountingFacade,
    ) = SessionConfig().run {
        val compactionFactory =
            sessionCompactionFactory(
                chatClientFactory = chatClientFactory,
                tokenFacade = tokenFacade,
                tokenEstimatorSelector = TokenEstimatorSelector(GptTokenEstimator(), HeuristicTokenEstimator()),
                properties = SessionCompactionProperties(),
            )
        sessionMemoryAdvisorFactory(
            sessionService = mockk<SessionService>(),
            dynamicChatModelService = dynamicChatModelService,
            sessionCompactionFactory = compactionFactory,
            sessionMemoryScheduler = Schedulers.immediate(),
            eventIdGenerator = sessionEventIdGenerator(),
        )
    }

    private fun field(
        target: Any?,
        name: String,
    ): Any? =
        requireNotNull(target)
            .javaClass
            .getDeclaredField(name)
            .also { it.isAccessible = true }
            .get(target)
}
