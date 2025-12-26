package com.ntgjvmagent.orchestrator.dto.response

import java.util.UUID

data class ProviderNameResponseDto(
    val id: UUID,
    val name: String,
    val active: Boolean,
)
