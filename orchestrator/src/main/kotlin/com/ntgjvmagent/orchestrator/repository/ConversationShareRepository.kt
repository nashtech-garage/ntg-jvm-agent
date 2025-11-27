package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.ConversationShareEntity
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface ConversationShareRepository : JpaRepository<ConversationShareEntity, UUID> {

    @EntityGraph(attributePaths = ["conversation"])
    fun findByShareToken(shareToken: String): Optional<ConversationShareEntity>

    @Query("SELECT cs FROM ConversationShareEntity cs WHERE cs.conversation.id = ?1 AND cs.isExpired = false")
    fun findActiveSharesByConversationId(conversationId: UUID): List<ConversationShareEntity>

    @Query("SELECT cs FROM ConversationShareEntity cs WHERE cs.sharedByUsername = ?1 AND cs.isExpired = false")
    fun findActiveSharesByUsername(username: String): List<ConversationShareEntity>

    @Query("SELECT cs FROM ConversationShareEntity cs WHERE cs.conversation.id = ?1 AND cs.sharedByUsername = ?2")
    fun findByConversationIdAndSharedBy(conversationId: UUID, username: String): List<ConversationShareEntity>
}

