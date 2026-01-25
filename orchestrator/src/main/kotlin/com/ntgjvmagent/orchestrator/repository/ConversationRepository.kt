package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.Conversation
import com.ntgjvmagent.orchestrator.viewmodel.ConversationResponseVm
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ConversationRepository : JpaRepository<Conversation, UUID> {
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
