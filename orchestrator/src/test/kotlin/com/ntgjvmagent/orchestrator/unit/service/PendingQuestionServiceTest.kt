package com.ntgjvmagent.orchestrator.unit.service

import com.ntgjvmagent.orchestrator.config.PendingQuestionProperties
import com.ntgjvmagent.orchestrator.dto.UserQuestionDto
import com.ntgjvmagent.orchestrator.dto.UserQuestionOptionDto
import com.ntgjvmagent.orchestrator.entity.PendingQuestion
import com.ntgjvmagent.orchestrator.exception.BadRequestException
import com.ntgjvmagent.orchestrator.model.PendingQuestionStatus
import com.ntgjvmagent.orchestrator.repository.PendingQuestionRepository
import com.ntgjvmagent.orchestrator.service.PendingQuestionService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.ToolResponseMessage
import org.springframework.ai.session.SessionEvent
import org.springframework.ai.session.SessionService
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class PendingQuestionServiceTest {
    private val repository = mockk<PendingQuestionRepository>()
    private val objectMapper = JsonMapper.builder().findAndAddModules().build()
    private val sessionService = mockk<SessionService>(relaxed = true)
    private val now = Instant.parse("2026-09-20T10:00:00Z")
    private val service =
        PendingQuestionService(
            repository,
            objectMapper,
            PendingQuestionProperties(Duration.ofHours(1)),
            sessionService,
            Clock.fixed(now, ZoneOffset.UTC),
        )

    @Test
    fun `resolution is committed only after the resumed stream succeeds`() {
        val questionId = UUID.randomUUID()
        val conversationId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val question = question()
        val entity =
            PendingQuestion(
                sessionId = UUID.randomUUID(),
                conversationId = conversationId,
                userId = userId,
                agentId = UUID.randomUUID(),
                correlationId = "chat-test",
                questionsJson = objectMapper.writeValueAsString(listOf(question)),
                expiresAt = now.plusSeconds(600),
            ).also { it.id = questionId }

        every { repository.findByIdAndUserIdForUpdate(questionId, userId) } returns entity
        every { repository.save(any()) } answers { firstArg() }

        val resolution =
            service.beginResolution(
                questionId,
                conversationId,
                userId,
                mapOf(question.question to "Require approval"),
            )

        assertEquals(PendingQuestionStatus.ANSWERING, entity.status)
        assertEquals(null, entity.answeredAt)
        assertEquals(entity.sessionId, resolution.sessionId)
        assertThrows<BadRequestException> {
            service.beginResolution(
                questionId,
                conversationId,
                userId,
                mapOf(question.question to "Skip approval"),
            )
        }
        service.completeResolution(questionId, userId)
        assertEquals(PendingQuestionStatus.RESOLVED, entity.status)
        assertEquals(now, entity.answeredAt)
        assertEquals("Require approval", resolution.prompt.substringAfterLast("? ").substringBefore('\n'))
    }

    @Test
    fun `failed resumed stream reopens the same question for retry`() {
        val questionId = UUID.randomUUID()
        val conversationId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val question = question()
        val entity =
            PendingQuestion(
                sessionId = UUID.randomUUID(),
                conversationId = conversationId,
                userId = userId,
                agentId = UUID.randomUUID(),
                correlationId = "chat-retry",
                questionsJson = objectMapper.writeValueAsString(listOf(question)),
                expiresAt = now.plusSeconds(600),
            ).also { it.id = questionId }

        every { repository.findByIdAndUserIdForUpdate(questionId, userId) } returns entity
        every { repository.save(any()) } answers { firstArg() }

        service.beginResolution(questionId, conversationId, userId, mapOf(question.question to "Staging"))
        service.reopenResolution(questionId, userId)

        assertEquals(PendingQuestionStatus.PENDING, entity.status)
        assertEquals(null, entity.answersJson)
        val retried =
            service.beginResolution(questionId, conversationId, userId, mapOf(question.question to "Production"))
        assertEquals(PendingQuestionStatus.ANSWERING, entity.status)
        assertEquals("Production", retried.prompt.substringAfterLast("? ").substringBefore('\n'))
    }

    @Test
    fun `resolution repairs an interrupted question tool response`() {
        val questionId = UUID.randomUUID()
        val conversationId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val question = question()
        val toolCall =
            AssistantMessage.ToolCall(
                "ask-user-call",
                "function",
                "AskUserQuestionTool",
                "{}",
            )
        val entity =
            PendingQuestion(
                sessionId = sessionId,
                conversationId = conversationId,
                userId = userId,
                agentId = UUID.randomUUID(),
                correlationId = "chat-repair",
                questionsJson = objectMapper.writeValueAsString(listOf(question)),
                expiresAt = now.plusSeconds(600),
            ).also { it.id = questionId }
        every { repository.findByIdAndUserIdForUpdate(questionId, userId) } returns entity
        every { repository.save(any()) } answers { firstArg() }
        every { sessionService.getEvents(sessionId.toString(), any()) } returns
            listOf(
                SessionEvent
                    .builder()
                    .sessionId(sessionId.toString())
                    .message(
                        AssistantMessage
                            .builder()
                            .content("")
                            .toolCalls(listOf(toolCall))
                            .build(),
                    ).build(),
            )
        val repaired = slot<SessionEvent>()

        service.beginResolution(
            questionId,
            conversationId,
            userId,
            mapOf(question.question to "Require approval"),
        )

        verify(exactly = 1) { sessionService.appendEvent(capture(repaired)) }
        val response = (repaired.captured.message as ToolResponseMessage).responses.single()
        assertEquals(toolCall.id(), response.id())
        assertEquals(toolCall.name(), response.name())
    }

    private fun question() =
        UserQuestionDto(
            question = "Should this require approval?",
            header = "Approval",
            options =
                listOf(
                    UserQuestionOptionDto("Require approval", "Ask before changing data"),
                    UserQuestionOptionDto("Skip approval", "Apply immediately"),
                ),
            multiSelect = false,
        )
}
