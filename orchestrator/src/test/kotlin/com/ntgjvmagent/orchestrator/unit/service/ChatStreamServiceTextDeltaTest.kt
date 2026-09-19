package com.ntgjvmagent.orchestrator.unit.service

import com.ntgjvmagent.orchestrator.service.chatTextDelta
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClientResponse
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.model.Generation
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChatStreamServiceTextDeltaTest {
    @Test
    fun `preserves whitespace-only streaming deltas`() {
        assertEquals(" ", chatTextDelta(response(" ")))
    }

    @Test
    fun `drops empty streaming deltas`() {
        assertNull(chatTextDelta(response("")))
    }

    private fun response(content: String): ChatClientResponse =
        ChatClientResponse
            .builder()
            .chatResponse(ChatResponse(listOf(Generation(AssistantMessage(content)))))
            .build()
}
