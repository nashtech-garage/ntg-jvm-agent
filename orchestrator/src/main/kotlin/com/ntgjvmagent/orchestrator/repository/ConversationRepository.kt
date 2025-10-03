package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.ConversationEntity
import com.ntgjvmagent.orchestrator.viewmodel.ConversationResponseVm
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ConversationRepository : JpaRepository<ConversationEntity, UUID> {
    @Query(
        """
        SELECT c.id AS id,
               c.title AS title,
               c.createdAt AS createdAt
        FROM ConversationEntity c
        WHERE c.username = :userName
        AND c.isActive = true
        ORDER BY c.createdAt ASC
    """,
    )
    fun listActiveConversationsByUser(
        @Param("userName") userName: String,
    ): List<ConversationResponseVm>
}
