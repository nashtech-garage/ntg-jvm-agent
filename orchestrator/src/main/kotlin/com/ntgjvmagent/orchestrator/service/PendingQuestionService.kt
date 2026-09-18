package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.config.PendingQuestionProperties
import com.ntgjvmagent.orchestrator.dto.PendingQuestionDto
import com.ntgjvmagent.orchestrator.dto.PendingQuestionResolution
import com.ntgjvmagent.orchestrator.dto.UserQuestionDto
import com.ntgjvmagent.orchestrator.entity.PendingQuestion
import com.ntgjvmagent.orchestrator.exception.BadRequestException
import com.ntgjvmagent.orchestrator.exception.ResourceNotFoundException
import com.ntgjvmagent.orchestrator.model.PendingQuestionStatus
import com.ntgjvmagent.orchestrator.repository.PendingQuestionRepository
import com.ntgjvmagent.orchestrator.tool.LocalToolCatalog
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.ToolResponseMessage
import org.springframework.ai.session.EventFilter
import org.springframework.ai.session.SessionEvent
import org.springframework.ai.session.SessionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
@Suppress("TooManyFunctions")
class PendingQuestionService(
    private val repository: PendingQuestionRepository,
    private val objectMapper: ObjectMapper,
    private val properties: PendingQuestionProperties,
    private val sessionService: SessionService,
    private val clock: Clock = Clock.systemUTC(),
) {
    @Transactional
    fun create(
        userId: UUID,
        agentId: UUID,
        sessionId: UUID,
        conversationId: UUID?,
        correlationId: String,
        questions: List<UserQuestionDto>,
    ): PendingQuestionDto {
        require(questions.isNotEmpty()) { "At least one question is required" }
        repository.cancelPendingForSession(sessionId)
        val saved =
            repository.save(
                PendingQuestion(
                    sessionId = sessionId,
                    conversationId = conversationId,
                    userId = userId,
                    agentId = agentId,
                    correlationId = correlationId,
                    questionsJson = objectMapper.writeValueAsString(questions),
                    expiresAt = clock.instant().plus(properties.ttl),
                ),
            )
        return saved.toDto()
    }

    @Transactional
    fun attachConversation(
        questionId: UUID,
        conversationId: UUID,
        userId: UUID,
    ): PendingQuestionDto {
        val pending = findForUpdate(questionId, userId)
        if (pending.status != PendingQuestionStatus.PENDING) {
            throw BadRequestException("Question is no longer pending")
        }
        pending.conversationId?.let {
            if (it != conversationId) throw BadRequestException("Question belongs to another conversation")
        }
        pending.conversationId = conversationId
        return repository.save(pending).toDto()
    }

    @Transactional
    fun beginResolution(
        questionId: UUID,
        conversationId: UUID,
        userId: UUID,
        answers: Map<String, String>,
    ): PendingQuestionResolution {
        val pending = findForUpdate(questionId, userId)
        if (pending.conversationId != conversationId) {
            throw ResourceNotFoundException("Pending question not found")
        }
        val now = clock.instant()
        if (pending.status != PendingQuestionStatus.PENDING) {
            throw BadRequestException("Question has already been resolved")
        }
        if (!pending.expiresAt.isAfter(now)) {
            pending.status = PendingQuestionStatus.EXPIRED
            repository.save(pending)
            throw BadRequestException("Question has expired")
        }

        val questions = pending.questions()
        validateAnswers(questions, answers)
        pending.answersJson = objectMapper.writeValueAsString(answers)
        pending.status = PendingQuestionStatus.ANSWERING
        repository.save(pending)
        repairInterruptedQuestionToolResponse(pending)

        return PendingQuestionResolution(
            sessionId = pending.sessionId,
            agentId = pending.agentId,
            prompt = buildResumePrompt(questions, answers),
        )
    }

    @Transactional
    fun completeResolution(
        questionId: UUID,
        userId: UUID,
    ): Boolean {
        val pending = findForUpdate(questionId, userId)
        if (pending.status != PendingQuestionStatus.ANSWERING) return false

        pending.status = PendingQuestionStatus.RESOLVED
        pending.answeredAt = clock.instant()
        repository.save(pending)
        return true
    }

    @Transactional
    fun reopenResolution(
        questionId: UUID,
        userId: UUID,
    ): Boolean {
        val pending = findForUpdate(questionId, userId)
        if (pending.status != PendingQuestionStatus.ANSWERING) return false

        pending.status = PendingQuestionStatus.PENDING
        pending.answersJson = null
        pending.answeredAt = null
        repository.save(pending)
        return true
    }

    @Transactional
    fun cancelForConversation(
        conversationId: UUID,
        userId: UUID,
    ): Int = repository.cancelPendingForConversation(conversationId, userId)

    @Transactional
    fun expireDue(): Int = repository.expireDue(clock.instant())

    @Transactional
    fun findPending(
        conversationId: UUID,
        userId: UUID,
    ): PendingQuestionDto? {
        val pending =
            repository.findFirstByConversationIdAndUserIdAndStatusOrderByCreatedAtDesc(
                conversationId,
                userId,
                PendingQuestionStatus.PENDING,
            ) ?: return null
        return if (!pending.expiresAt.isAfter(clock.instant())) {
            pending.status = PendingQuestionStatus.EXPIRED
            repository.save(pending)
            null
        } else {
            pending.toDto()
        }
    }

    private fun findForUpdate(
        questionId: UUID,
        userId: UUID,
    ): PendingQuestion =
        repository.findByIdAndUserIdForUpdate(questionId, userId)
            ?: throw ResourceNotFoundException("Pending question not found")

    private fun PendingQuestion.questions(): List<UserQuestionDto> =
        objectMapper.readValue(questionsJson, Array<UserQuestionDto>::class.java).toList()

    private fun PendingQuestion.toDto() =
        PendingQuestionDto(
            id = requireNotNull(id),
            conversationId = conversationId,
            questions = questions(),
            expiresAt = expiresAt,
        )

    private fun validateAnswers(
        questions: List<UserQuestionDto>,
        answers: Map<String, String>,
    ) {
        val expected = questions.map(UserQuestionDto::question).toSet()
        if (answers.keys != expected || answers.values.any(String::isBlank)) {
            throw BadRequestException("Every pending question must have a non-blank answer")
        }
    }

    private fun repairInterruptedQuestionToolResponse(pending: PendingQuestion) {
        val sessionId = pending.sessionId.toString()
        val events = sessionService.getEvents(sessionId, EventFilter.active())
        val indexedCall =
            events
                .asSequence()
                .mapIndexedNotNull { index, event ->
                    (event.message as? AssistantMessage)
                        ?.toolCalls
                        ?.lastOrNull { it.name() == LocalToolCatalog.ASK_USER_QUESTION_TOOL_NAME }
                        ?.let { index to it }
                }.lastOrNull() ?: return
        val (callIndex, toolCall) = indexedCall
        val responseExists =
            events
                .drop(callIndex + 1)
                .asSequence()
                .mapNotNull { it.message as? ToolResponseMessage }
                .flatMap { it.responses.asSequence() }
                .any { it.id() == toolCall.id() }
        if (responseExists) return

        val response =
            ToolResponseMessage.ToolResponse(
                toolCall.id(),
                toolCall.name(),
                "The user question was presented. The answer follows in the next user message.",
            )
        sessionService.appendEvent(
            SessionEvent
                .builder()
                .id("pending-question-recovery-${pending.id}-${toolCall.id()}")
                .sessionId(sessionId)
                .message(ToolResponseMessage.builder().responses(listOf(response)).build())
                .build(),
        )
    }

    private fun buildResumePrompt(
        questions: List<UserQuestionDto>,
        answers: Map<String, String>,
    ): String =
        buildString {
            appendLine("The user answered the pending clarification questions:")
            questions.forEach { question ->
                append("- ")
                    .append(question.question)
                    .append(" ")
                    .appendLine(answers.getValue(question.question))
            }
            append("Continue the original request using these answers.")
        }
}
