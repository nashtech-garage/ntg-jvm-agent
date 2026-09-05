package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.Conversation
import com.ntgjvmagent.orchestrator.viewmodel.ConversationResponseVm
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface ConversationRepository : JpaRepository<Conversation, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Conversation c WHERE c.id = :conversationId")
    fun findByIdForSessionUpdate(
        @Param("conversationId") conversationId: UUID,
    ): Conversation?

    /**
     * Session ids that no conversation points at any more.
     *
     * A session is created before the conversation row exists, so a stream that fails on the
     * first message leaves one behind; [createdBefore] keeps the in-flight ones out of the
     * result. It is bound as a LocalDateTime because the session library writes created_at with
     * `Timestamp.from(...)`, i.e. rendered in the JVM zone into a column without one -- binding
     * an Instant would let Hibernate normalise to UTC and shift the comparison.
     */
    @Query(
        value = """
        SELECT s.id
        FROM ai_session s
        LEFT JOIN conversation c ON CAST(c.session_id AS text) = s.id
        WHERE c.id IS NULL
          AND s.created_at < :createdBefore
        ORDER BY s.created_at
        LIMIT :limit
    """,
        nativeQuery = true,
    )
    fun findOrphanSessionIds(
        @Param("createdBefore") createdBefore: LocalDateTime,
        @Param("limit") limit: Int,
    ): List<String>

    @Query(
        """
        SELECT c.id AS id,
               c.title AS title,
               c.createdAt AS createdAt
        FROM Conversation c
        WHERE c.createdBy.id = :userId
        AND c.isActive = true
        ORDER BY c.createdAt DESC
    """,
    )
    fun listActiveConversationsByUser(
        @Param("userId") userId: UUID,
    ): List<ConversationResponseVm>
}
