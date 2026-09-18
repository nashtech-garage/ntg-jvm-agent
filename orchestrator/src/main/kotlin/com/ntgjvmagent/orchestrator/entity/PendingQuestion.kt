package com.ntgjvmagent.orchestrator.entity

import com.ntgjvmagent.orchestrator.entity.base.BaseEntity
import com.ntgjvmagent.orchestrator.model.PendingQuestionStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "pending_question")
@Suppress("LongParameterList")
class PendingQuestion(
    @Column(name = "session_id", nullable = false)
    val sessionId: UUID,
    @Column(name = "conversation_id")
    var conversationId: UUID?,
    @Column(name = "user_id", nullable = false)
    val userId: UUID,
    @Column(name = "agent_id", nullable = false)
    val agentId: UUID,
    @Column(name = "correlation_id", nullable = false)
    val correlationId: String,
    @Column(name = "questions_json", nullable = false, columnDefinition = "TEXT")
    val questionsJson: String,
    @Column(name = "answers_json", columnDefinition = "TEXT")
    var answersJson: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PendingQuestionStatus = PendingQuestionStatus.PENDING,
    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,
    @Column(name = "answered_at")
    var answeredAt: Instant? = null,
) : BaseEntity()
