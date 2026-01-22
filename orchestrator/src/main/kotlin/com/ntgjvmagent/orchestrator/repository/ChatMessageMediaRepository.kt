package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.ChatMessageMedia
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ChatMessageMediaRepository : JpaRepository<ChatMessageMedia, UUID>
