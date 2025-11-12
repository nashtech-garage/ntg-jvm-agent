package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.ConversationSummaryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ConversationSummaryRepository : JpaRepository<ConversationSummaryEntity, UUID>
