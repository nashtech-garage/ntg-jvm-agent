package com.ntgjvmagent.orchestrator.unit.chat

import com.ntgjvmagent.orchestrator.component.AgentChatClientFactory
import com.ntgjvmagent.orchestrator.config.ToolCallingConfig
import com.ntgjvmagent.orchestrator.dto.request.ConversationIntentRequestDto
import com.ntgjvmagent.orchestrator.dto.response.AgentResponseDto
import com.ntgjvmagent.orchestrator.dto.response.ConversationIntentResponseDto
import com.ntgjvmagent.orchestrator.service.ConversationIntentService
import com.ntgjvmagent.orchestrator.service.DynamicChatModelService
import com.ntgjvmagent.orchestrator.token.accounting.TokenAccountingFacade
import io.micrometer.observation.ObservationRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.converter.BeanOutputConverter
import reactor.core.publisher.Flux
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatClientStructuredOutputTest {
    private val agentId = UUID.randomUUID()
    private val userId = UUID.randomUUID()

    @Test
    fun `normal ChatClient call returns plain text`() {
        val model = RecordingChatModel("I can answer questions and use attached knowledge.")
        val chatClientFactory = createFactory(model)

        val answer =
            chatClientFactory
                .create(agentId)
                .prompt()
                .user("Explain what capabilities you have.")
                .call()
                .content()

        assertEquals("I can answer questions and use attached knowledge.", answer)
        assertTrue(model.lastPrompt.contents.contains("Explain what capabilities you have."))
    }

    @Test
    fun `structured call returns typed intent and injects its schema`() {
        val model =
            RecordingChatModel(
                """
                {
                  "category": "KNOWLEDGE_LOOKUP",
                  "requiresKnowledge": true,
                  "confidence": 0.96,
                  "rationale": "The request asks about an enterprise leave policy."
                }
                """.trimIndent(),
            )
        val tokenFacade = relaxedTokenFacade()
        val service = createService(model, tokenFacade)

        val result =
            service.classify(
                userId,
                ConversationIntentRequestDto(
                    agentId = agentId,
                    question = "How many vacation days can I carry over under the company leave policy?",
                    correlationId = "intent-contract",
                ),
            )

        assertEquals(ConversationIntentResponseDto.Category.KNOWLEDGE_LOOKUP, result.category)
        assertTrue(result.requiresKnowledge)
        assertEquals(0.96, result.confidence)
        assertTrue(model.lastPrompt.contents.contains("requiresKnowledge"))
        assertTrue(model.lastPrompt.contents.contains("KNOWLEDGE_LOOKUP"))
        verify(exactly = 1) { tokenFacade.recordWithFallback(any(), any()) }
    }

    @Test
    fun `schema and deterministic parsing keep the canonical fields`() {
        val converter = BeanOutputConverter(ConversationIntentResponseDto::class.java)
        val schema = converter.jsonSchemaMap
        val properties = schema["properties"] as Map<*, *>

        assertEquals(
            setOf("category", "requiresKnowledge", "confidence", "rationale"),
            properties.keys,
        )

        val parsed =
            converter.convert(
                """
                {
                  "category": "ACCOUNT_ACTION",
                  "requiresKnowledge": false,
                  "confidence": 0.81,
                  "rationale": "The user wants to change an account setting."
                }
                """.trimIndent(),
            )

        assertEquals(ConversationIntentResponseDto.Category.ACCOUNT_ACTION, parsed.category)
        assertFalse(parsed.requiresKnowledge)
        assertEquals(0.81, parsed.confidence)
    }

    @Test
    fun `semantically inconsistent typed values use the documented fallback`() {
        val model =
            RecordingChatModel(
                """
                {
                  "category": "GENERAL_CHAT",
                  "requiresKnowledge": true,
                  "confidence": 0.9,
                  "rationale": "The flags conflict with the category."
                }
                """.trimIndent(),
            )
        val result =
            createService(model, relaxedTokenFacade()).classify(
                userId,
                ConversationIntentRequestDto(agentId, "Hello"),
            )

        assertEquals(ConversationIntentResponseDto.fallback(), result)
    }

    @Test
    fun `malformed structured output uses the fallback and remains accounted`() {
        val tokenFacade = relaxedTokenFacade()
        val result =
            createService(RecordingChatModel("not-json"), tokenFacade).classify(
                userId,
                ConversationIntentRequestDto(agentId, "Hello"),
            )

        assertEquals(ConversationIntentResponseDto.fallback(), result)
        verify(exactly = 1) { tokenFacade.recordWithFallback(any(), any()) }
    }

    private fun createService(
        model: ChatModel,
        tokenFacade: TokenAccountingFacade,
    ): ConversationIntentService {
        val dynamicChatModelService = dynamicChatModelService(model)
        return ConversationIntentService(
            createFactory(model, dynamicChatModelService),
            dynamicChatModelService,
            tokenFacade,
        )
    }

    private fun createFactory(
        model: ChatModel,
        dynamicChatModelService: DynamicChatModelService = dynamicChatModelService(model),
    ): AgentChatClientFactory =
        AgentChatClientFactory(
            dynamicChatModelService,
            ObservationRegistry.NOOP,
            ToolCallingConfig().toolCallingAdvisorBuilder(ObservationRegistry.NOOP),
        )

    private fun dynamicChatModelService(model: ChatModel): DynamicChatModelService {
        val agentConfig = mockk<AgentResponseDto> { every { this@mockk.model } returns "contract-model" }
        return mockk {
            every { getChatModel(agentId) } returns model
            every { getAgentConfig(agentId) } returns agentConfig
        }
    }

    private fun relaxedTokenFacade(): TokenAccountingFacade =
        mockk(relaxed = true) {
            every { estimatePromptInput(any(), any()) } returns 37
        }

    private class RecordingChatModel(
        private val responseText: String,
    ) : ChatModel {
        lateinit var lastPrompt: Prompt

        override fun call(prompt: Prompt): ChatResponse {
            lastPrompt = prompt
            return ChatResponse(listOf(Generation(AssistantMessage(responseText))))
        }

        override fun stream(prompt: Prompt): Flux<ChatResponse> = Flux.just(call(prompt))
    }
}
