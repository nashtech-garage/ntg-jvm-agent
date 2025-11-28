package com.ntgjvmagent.orchestrator.dto

import java.util.UUID

data class AgentListResponseDto(
    val id: UUID,
    val name: String,
    val model: String,
    val lastModifiedBy: String,
    val lastModifiedWhen: String,
    val lastPublishedWhen: String?,
    val owner: String,
    val status: String,
)
