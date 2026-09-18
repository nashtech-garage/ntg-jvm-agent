package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.PendingQuestion
import com.ntgjvmagent.orchestrator.model.PendingQuestionStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface PendingQuestionRepository : JpaRepository<PendingQuestion, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM PendingQuestion q WHERE q.id = :id AND q.userId = :userId")
    fun findByIdAndUserIdForUpdate(
        @Param("id") id: UUID,
        @Param("userId") userId: UUID,
    ): PendingQuestion?

    fun findFirstByConversationIdAndUserIdAndStatusOrderByCreatedAtDesc(
        conversationId: UUID,
        userId: UUID,
        status: PendingQuestionStatus,
    ): PendingQuestion?

    @Modifying
    @Query(
        """
        UPDATE PendingQuestion q
           SET q.status = :cancelled
         WHERE q.sessionId = :sessionId
           AND q.status = :pending
        """,
    )
    fun cancelPendingForSession(
        @Param("sessionId") sessionId: UUID,
        @Param("pending") pending: PendingQuestionStatus = PendingQuestionStatus.PENDING,
        @Param("cancelled") cancelled: PendingQuestionStatus = PendingQuestionStatus.CANCELLED,
    ): Int

    @Modifying
    @Query(
        """
        UPDATE PendingQuestion q
           SET q.status = :cancelled
         WHERE q.conversationId = :conversationId
           AND q.userId = :userId
           AND q.status IN :activeStatuses
        """,
    )
    fun cancelPendingForConversation(
        @Param("conversationId") conversationId: UUID,
        @Param("userId") userId: UUID,
        @Param("activeStatuses") activeStatuses: Set<PendingQuestionStatus> =
            setOf(PendingQuestionStatus.PENDING, PendingQuestionStatus.ANSWERING),
        @Param("cancelled") cancelled: PendingQuestionStatus = PendingQuestionStatus.CANCELLED,
    ): Int

    @Modifying
    @Query(
        """
        UPDATE PendingQuestion q
           SET q.status = :expired
         WHERE q.status IN :activeStatuses
           AND q.expiresAt <= :now
        """,
    )
    fun expireDue(
        @Param("now") now: Instant,
        @Param("activeStatuses") activeStatuses: Set<PendingQuestionStatus> =
            setOf(PendingQuestionStatus.PENDING, PendingQuestionStatus.ANSWERING),
        @Param("expired") expired: PendingQuestionStatus = PendingQuestionStatus.EXPIRED,
    ): Int
}
