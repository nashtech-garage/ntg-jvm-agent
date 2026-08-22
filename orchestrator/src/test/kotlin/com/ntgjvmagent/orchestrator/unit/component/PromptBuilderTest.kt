package com.ntgjvmagent.orchestrator.unit.component

import com.ntgjvmagent.orchestrator.component.PromptBuilder
import com.ntgjvmagent.orchestrator.dto.ChatRequestDto
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PromptBuilderTest {
    private val promptBuilder = PromptBuilder()

    @Test
    fun `preserves summary question and recent history order`() {
        val request =
            ChatRequestDto(
                question = "What changed?",
                conversationId = UUID.randomUUID(),
                files = null,
                agentId = UUID.randomUUID(),
            )

        val prompt =
            promptBuilder.build(
                request = request,
                history = listOf("USER: first", "ASSISTANT: second"),
                summary = "Older context",
            )

        val expected =
            "Conversation summary so far:\n" +
                "Older context\n\n" +
                "User question: What changed?\n" +
                "Chat history:\n" +
                "USER: first\n" +
                "ASSISTANT: second\n\n"

        assertEquals(expected, prompt)
    }
}
