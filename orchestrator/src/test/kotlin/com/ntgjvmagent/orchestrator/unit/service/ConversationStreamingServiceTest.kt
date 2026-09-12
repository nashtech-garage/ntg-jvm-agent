package com.ntgjvmagent.orchestrator.unit.service

import com.ntgjvmagent.orchestrator.advisor.ToolCallEvent
import com.ntgjvmagent.orchestrator.dto.ChatRequestDto
import com.ntgjvmagent.orchestrator.dto.ChatResponseDto
import com.ntgjvmagent.orchestrator.model.ChatStreamEvent
import com.ntgjvmagent.orchestrator.service.ChatModelService
import com.ntgjvmagent.orchestrator.service.ConversationCommandService
import com.ntgjvmagent.orchestrator.service.ConversationSessionService
import com.ntgjvmagent.orchestrator.service.ConversationStreamingService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import java.util.UUID

class ConversationStreamingServiceTest {
    private val chatModelService = mockk<ChatModelService>()
    private val commandService = mockk<ConversationCommandService>()
    private val conversationSessionService = mockk<ConversationSessionService>()

    @Test
    fun `preserves the five-event SSE contract`() {
        val userId = UUID.randomUUID()
        val request = request()
        val toolEvent =
            ToolCallEvent(
                id = "call-1",
                name = "TodoWrite",
                phase = ToolCallEvent.Phase.COMPLETED,
                todoItems =
                    listOf(
                        ToolCallEvent.TodoItem(
                            content = "Verify the result",
                            status = "in_progress",
                            activeForm = "Verifying the result",
                        ),
                    ),
            )

        every { conversationSessionService.resolveSessionId(any(), any(), any(), any()) } returns UUID.randomUUID()
        every { commandService.createConversationWithFirstMessage(any(), any(), any(), any()) } returns
            mockk<ChatResponseDto>()
        every { chatModelService.call(any(), any(), any()) } returns
            Flux.just(
                ChatStreamEvent.Tool(toolEvent),
                ChatStreamEvent.Reasoning("checking policy"),
                ChatStreamEvent.Message("15 minutes"),
            )

        val successEvents = service().streamConversation(request, userId).collectList().block()!!

        every { chatModelService.call(any(), any(), any()) } returns
            Flux.error(IllegalStateException("provider unavailable"))

        val errorEvents = service().streamConversation(request, userId).collectList().block()!!

        assertEquals(
            setOf("message", "tool", "reasoning", "complete", "error"),
            (successEvents + errorEvents).mapNotNull { it.event() }.toSet(),
        )
        assertEquals(
            toolEvent,
            successEvents.single { it.event() == "tool" }.data(),
        )
    }

    private fun service() =
        ConversationStreamingService(
            chatModelService = chatModelService,
            commandService = commandService,
            conversationSessionService = conversationSessionService,
        )

    private fun request() =
        ChatRequestDto(
            question = "What is the response target?",
            conversationId = null,
            files = null,
            agentId = UUID.randomUUID(),
        )
}
