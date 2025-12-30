package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.ChatMessageEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface ChatMessageRepository : JpaRepository<ChatMessageEntity, UUID> {
    @Query(
"""
        SELECT m
        FROM ChatMessageEntity m
        LEFT JOIN FETCH m.messageMedias
        WHERE m.conversation.id = :conversationId
        ORDER BY m.createdAt ASC
        """,
    )
    fun listMessageByConversationId(
        @Param("conversationId") conversationId: UUID,
    ): List<ChatMessageEntity>

    fun findByConversationIdOrderByCreatedAtDesc(conversationId: UUID, pageable: Pageable) : List<ChatMessageEntity>

    @Query(
        """
        SELECT m
        FROM ChatMessageEntity m
        WHERE m.conversation.id = :conversationId
          AND m.createdAt < :cutoff
        ORDER BY m.createdAt ASC
        """
    )
    fun findMessagesBefore(
        @Param("conversationId") conversationId: UUID,
        @Param("cutoff") cutoff: Instant
    ): List<ChatMessageEntity>
}
