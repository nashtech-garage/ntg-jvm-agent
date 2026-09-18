package com.ntgjvmagent.orchestrator.unit.service

import com.ntgjvmagent.orchestrator.advisor.ToolCallEvent
import com.ntgjvmagent.orchestrator.dto.ChatRequestDto
import com.ntgjvmagent.orchestrator.dto.ChatResponseDto
import com.ntgjvmagent.orchestrator.dto.PendingQuestionDto
import com.ntgjvmagent.orchestrator.dto.PendingQuestionResolution
import com.ntgjvmagent.orchestrator.dto.UserQuestionDto
import com.ntgjvmagent.orchestrator.dto.UserQuestionOptionDto
import com.ntgjvmagent.orchestrator.dto.request.QuestionAnswerRequestDto
import com.ntgjvmagent.orchestrator.model.ChatStreamEvent
import com.ntgjvmagent.orchestrator.service.ChatModelService
import com.ntgjvmagent.orchestrator.service.ConversationCommandService
import com.ntgjvmagent.orchestrator.service.ConversationSessionService
import com.ntgjvmagent.orchestrator.service.ConversationStreamingService
import com.ntgjvmagent.orchestrator.service.PendingQuestionService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import java.time.Instant
import java.util.UUID

class ConversationStreamingServiceTest {
    private val chatModelService = mockk<ChatModelService>()
    private val commandService = mockk<ConversationCommandService>()
    private val conversationSessionService = mockk<ConversationSessionService>()
    private val pendingQuestionService = mockk<PendingQuestionService>()

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

    @Test
    fun `question is the terminal SSE event without complete`() {
        val userId = UUID.randomUUID()
        val request = request()
        val sessionId = UUID.randomUUID()
        val conversationId = UUID.randomUUID()
        val pending =
            PendingQuestionDto(
                id = UUID.randomUUID(),
                conversationId = null,
                questions =
                    listOf(
                        UserQuestionDto(
                            question = "Which environment?",
                            header = "Environment",
                            options =
                                listOf(
                                    UserQuestionOptionDto("Staging", "Use staging"),
                                    UserQuestionOptionDto("Production", "Use production"),
                                ),
                            multiSelect = false,
                        ),
                    ),
                expiresAt = Instant.now().plusSeconds(3600),
            )
        val attached = pending.copy(conversationId = conversationId)

        every { conversationSessionService.resolveSessionId(any(), any(), any(), any()) } returns sessionId
        every { chatModelService.call(userId, sessionId, any()) } returns
            Flux.just(ChatStreamEvent.Question(pending), ChatStreamEvent.Message("must not escape"))
        every { commandService.persistPendingQuestionTurn(userId, any(), sessionId) } returns
            conversationId
        every { pendingQuestionService.attachConversation(pending.id, conversationId, userId) } returns attached

        val events = service(pendingQuestionService).streamConversation(request, userId).collectList().block()!!

        assertEquals(listOf("question"), events.map { it.event() })
        assertEquals(attached, events.single().data())
    }

    @Test
    fun `answer resumes the same session and persists the user-facing answer`() {
        val userId = UUID.randomUUID()
        val conversationId = UUID.randomUUID()
        val questionId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val agentId = UUID.randomUUID()
        val question = "Which environment?"
        val answerRequest = QuestionAnswerRequestDto(questionId, mapOf(question to "Production"))
        val resolution =
            PendingQuestionResolution(
                sessionId = sessionId,
                agentId = agentId,
                prompt = "The user answered the pending clarification questions: Production",
            )

        every {
            pendingQuestionService.beginResolution(questionId, conversationId, userId, answerRequest.answers)
        } returns
            resolution
        every { pendingQuestionService.completeResolution(questionId, userId) } returns true
        every { conversationSessionService.resolveSessionId(conversationId, userId, agentId, any()) } returns sessionId
        every { chatModelService.call(userId, sessionId, any()) } returns Flux.just(ChatStreamEvent.Message("done"))
        every {
            commandService.appendConversationAnswerTurn(
                userId,
                any(),
                "$question: Production",
                "done",
            )
        } returns mockk<ChatResponseDto>()

        val events =
            service(pendingQuestionService)
                .answerQuestion(conversationId, answerRequest, userId)
                .collectList()
                .block()!!

        assertEquals(listOf("message", "complete"), events.map { it.event() })
        verify {
            pendingQuestionService.completeResolution(questionId, userId)
            commandService.appendConversationAnswerTurn(
                userId,
                any(),
                "$question: Production",
                "done",
            )
        }
    }

    @Test
    fun `failed resumed stream reopens the pending question`() {
        val userId = UUID.randomUUID()
        val conversationId = UUID.randomUUID()
        val questionId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val agentId = UUID.randomUUID()
        val answerRequest = QuestionAnswerRequestDto(questionId, mapOf("Which environment?" to "Production"))
        val resolution =
            PendingQuestionResolution(
                sessionId = sessionId,
                agentId = agentId,
                prompt = "Resume with Production",
            )

        every {
            pendingQuestionService.beginResolution(questionId, conversationId, userId, answerRequest.answers)
        } returns resolution
        every { pendingQuestionService.reopenResolution(questionId, userId) } returns true
        every { conversationSessionService.resolveSessionId(conversationId, userId, agentId, any()) } returns sessionId
        every { chatModelService.call(userId, sessionId, any()) } returns
            Flux.error(IllegalStateException("provider unavailable"))

        val events =
            service(pendingQuestionService)
                .answerQuestion(conversationId, answerRequest, userId)
                .collectList()
                .block()!!

        assertEquals(listOf("error"), events.map { it.event() })
        verify(exactly = 1) { pendingQuestionService.reopenResolution(questionId, userId) }
        verify(exactly = 0) { pendingQuestionService.completeResolution(any(), any()) }
    }

    @Test
    fun `cancelled resumed stream reopens the pending question`() {
        val userId = UUID.randomUUID()
        val conversationId = UUID.randomUUID()
        val questionId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val agentId = UUID.randomUUID()
        val answerRequest = QuestionAnswerRequestDto(questionId, mapOf("Which environment?" to "Production"))
        val resolution =
            PendingQuestionResolution(
                sessionId = sessionId,
                agentId = agentId,
                prompt = "Resume with Production",
            )

        every {
            pendingQuestionService.beginResolution(questionId, conversationId, userId, answerRequest.answers)
        } returns resolution
        every { pendingQuestionService.reopenResolution(questionId, userId) } returns true
        every { conversationSessionService.resolveSessionId(conversationId, userId, agentId, any()) } returns sessionId
        every { chatModelService.call(userId, sessionId, any()) } returns
            Flux.concat(Flux.just(ChatStreamEvent.Message("partial")), Flux.never())

        val events =
            service(pendingQuestionService)
                .answerQuestion(conversationId, answerRequest, userId)
                .take(1)
                .collectList()
                .block()!!

        assertEquals(listOf("message"), events.map { it.event() })
        verify(exactly = 1) { pendingQuestionService.reopenResolution(questionId, userId) }
        verify(exactly = 0) { pendingQuestionService.completeResolution(any(), any()) }
    }

    @Test
    fun `a new user turn cancels the pending question before continuing`() {
        val userId = UUID.randomUUID()
        val conversationId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val request = request().copy(conversationId = conversationId)

        every { pendingQuestionService.cancelForConversation(conversationId, userId) } returns 1
        every { conversationSessionService.resolveSessionId(conversationId, userId, request.agentId, any()) } returns
            sessionId
        every { chatModelService.call(userId, sessionId, any()) } returns
            Flux.just(ChatStreamEvent.Message("redirected"))
        every { commandService.appendConversationMessage(userId, any(), "redirected") } returns
            mockk<ChatResponseDto>()

        service(pendingQuestionService).streamConversation(request, userId).collectList().block()

        verify(exactly = 1) { pendingQuestionService.cancelForConversation(conversationId, userId) }
    }

    private fun service(pendingService: PendingQuestionService? = null) =
        ConversationStreamingService(
            chatModelService = chatModelService,
            commandService = commandService,
            conversationSessionService = conversationSessionService,
            pendingQuestionService = pendingService,
        )

    private fun request() =
        ChatRequestDto(
            question = "What is the response target?",
            conversationId = null,
            files = null,
            agentId = UUID.randomUUID(),
        )
}
