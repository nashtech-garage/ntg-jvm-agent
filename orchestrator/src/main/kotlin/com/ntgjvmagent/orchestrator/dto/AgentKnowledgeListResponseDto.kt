package com.ntgjvmagent.orchestrator.dto

import java.util.UUID

data class AgentKnowledgeListResponseDto(
    val id: UUID,
    val name: String,
    val type: String?,
    val availableTo: String,
    val usage: String?,
    val lastModifiedBy: String,
    val lastModifiedWhen: String,
    val status: String,
)
