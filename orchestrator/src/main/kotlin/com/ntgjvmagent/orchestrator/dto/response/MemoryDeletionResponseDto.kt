package com.ntgjvmagent.orchestrator.dto.response

data class MemoryDeletionResponseDto(
    val deletedCount: Int,
    val logRetentionNotice: String,
)
