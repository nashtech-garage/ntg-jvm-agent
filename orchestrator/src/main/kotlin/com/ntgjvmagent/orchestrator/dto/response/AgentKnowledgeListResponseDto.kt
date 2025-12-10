package com.ntgjvmagent.orchestrator.dto.response

import com.ntgjvmagent.orchestrator.model.KnowledgeSourceType
import com.ntgjvmagent.orchestrator.model.KnowledgeStatus
import java.util.UUID

data class AgentKnowledgeListResponseDto(
    val id: UUID,
    val name: String,
    val type: KnowledgeSourceType,
    val availableTo: String,
    val usage: String?,
    val lastModifiedBy: String?,
    val lastModifiedWhen: String?,
    val status: KnowledgeStatus,
)
