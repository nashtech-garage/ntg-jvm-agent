package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.ConversationShare
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface ConversationShareRepository : JpaRepository<ConversationShare, UUID> {
    @EntityGraph(attributePaths = ["conversation"])
    fun findByShareToken(shareToken: String): Optional<ConversationShare>
}
