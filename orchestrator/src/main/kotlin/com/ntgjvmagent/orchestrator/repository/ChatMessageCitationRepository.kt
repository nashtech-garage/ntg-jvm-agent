package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.ChatMessageCitationEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ChatMessageCitationRepository : JpaRepository<ChatMessageCitationEntity, UUID>
