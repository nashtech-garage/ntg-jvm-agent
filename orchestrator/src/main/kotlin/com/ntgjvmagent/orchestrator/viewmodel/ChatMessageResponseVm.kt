package com.ntgjvmagent.orchestrator.viewmodel

import com.ntgjvmagent.orchestrator.dto.CitationDto
import java.time.Instant
import java.util.UUID

data class ChatMessageResponseVm(
    val id: UUID,
    val content: String,
    val createdAt: Instant,
    val type: Int,
    val medias: List<ChatMessageMediaVm>,
    val citations: List<CitationDto>,
)

data class ChatMessageMediaVm(
    val fileName: String,
    val contentType: String,
    val data: String,
)
