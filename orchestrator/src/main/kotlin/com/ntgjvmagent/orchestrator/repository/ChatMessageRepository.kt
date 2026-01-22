package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.ChatMessage
import com.ntgjvmagent.orchestrator.model.ChatMessageType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ChatMessageRepository : JpaRepository<ChatMessage, UUID> {
    @Query(
        """
    SELECT m
    FROM ChatMessage m
    WHERE m.conversation.id = :conversationId
    ORDER BY m.createdAt ASC,
             CASE WHEN m.type = :questionType THEN 0 ELSE 1 END
    """,
    )
    fun listMessageByConversationIdOrdered(
        @Param("conversationId") conversationId: UUID,
        @Param("questionType") questionType: ChatMessageType = ChatMessageType.QUESTION,
    ): List<ChatMessage>
}
