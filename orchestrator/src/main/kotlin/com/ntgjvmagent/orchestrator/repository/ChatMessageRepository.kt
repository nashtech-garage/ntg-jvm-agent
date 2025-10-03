package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.ChatMessageEntity
import com.ntgjvmagent.orchestrator.viewmodel.ChatMessageResponseVm
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ChatMessageRepository : JpaRepository<ChatMessageEntity, UUID> {
    @Query(
"""
        SELECT m.id AS id,
               m.content AS content,
               m.createdAt AS createdAt,
               m.type AS type
        FROM ChatMessageEntity m
        WHERE m.conversation.id = :conversationId
        ORDER BY m.createdAt ASC
    """,
    )
    fun listMessageByConversationId(
        @Param("conversationId") conversationId: UUID,
    ): List<ChatMessageResponseVm>
}
