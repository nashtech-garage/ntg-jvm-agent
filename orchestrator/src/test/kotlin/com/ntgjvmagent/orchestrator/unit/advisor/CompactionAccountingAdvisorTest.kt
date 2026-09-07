package com.ntgjvmagent.orchestrator.unit.advisor

import com.ntgjvmagent.orchestrator.advisor.CompactionAccountingAdvisor
import com.ntgjvmagent.orchestrator.model.TokenOperation
import com.ntgjvmagent.orchestrator.token.accounting.LlmAccountingContext
import com.ntgjvmagent.orchestrator.token.accounting.TokenAccountingFacade
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClientRequest
import org.springframework.ai.chat.client.ChatClientResponse
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.ToolResponseMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.content.MediaContent
import org.springframework.ai.tokenizer.TokenCountEstimator
import java.util.UUID

class CompactionAccountingAdvisorTest {
    @Test
    fun `system prompt and tool responses count towards the compaction budget`() {
        val tokenFacade = mockk<TokenAccountingFacade>(relaxed = true)
        val estimated = slot<Int>()
        every {
            tokenFacade.assertInputBudget(any(), TokenOperation.SUMMARIZATION, capture(estimated))
        } returns Unit

        val prompt =
            Prompt(
                listOf(
                    SystemMessage("summarize"), // 9 - not a MediaContent
                    UserMessage("history"), // 7
                    ToolResponseMessage
                        .builder()
                        .responses(listOf(ToolResponseMessage.ToolResponse("id", "lookup", "data")))
                        .build(), // "lookup data" -> 11, not a MediaContent
                ),
            )

        advisor(tokenFacade).adviseCall(request(prompt), chain())

        assertEquals(27, estimated.captured)
    }

    @Test
    fun `recorded input text matches what was estimated`() {
        val tokenFacade = mockk<TokenAccountingFacade>(relaxed = true)
        val recorded = slot<LlmAccountingContext>()
        every { tokenFacade.recordWithFallback(capture(recorded), any()) } returns Unit
        val prompt = Prompt(listOf(SystemMessage("summarize"), UserMessage("history")))

        advisor(tokenFacade).adviseCall(request(prompt), chain())

        assertEquals("summarize\nhistory", recorded.captured.userInputText)
        assertEquals(recorded.captured.userInputText.length - 1, recorded.captured.estimatedInputTokens)
    }

    private fun advisor(tokenFacade: TokenAccountingFacade) =
        CompactionAccountingAdvisor(
            userId = UUID.randomUUID(),
            agentId = UUID.randomUUID(),
            model = "gpt-4o-mini",
            rootCorrelationId = "chat-root",
            tokenFacade = tokenFacade,
            tokenCountEstimator = LengthTokenCountEstimator,
        )

    private fun request(prompt: Prompt) = ChatClientRequest.builder().prompt(prompt).build()

    private fun chain(): CallAdvisorChain =
        mockk {
            every { nextCall(any()) } returns
                ChatClientResponse
                    .builder()
                    .chatResponse(ChatResponse(listOf(Generation(AssistantMessage("summary")))))
                    .build()
        }

    /** One token per character, so an assertion can pin down exactly which messages were counted. */
    private object LengthTokenCountEstimator : TokenCountEstimator {
        override fun estimate(text: String?): Int = text?.length ?: 0

        override fun estimate(content: MediaContent): Int = estimate(content.text)

        override fun estimate(contents: Iterable<MediaContent>): Int = contents.sumOf(::estimate)
    }
}
