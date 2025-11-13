package com.ntgjvmagent.orchestrator.viewmodel

import java.time.OffsetDateTime
import java.util.UUID

data class ChatMessageResponseVm(
    val id: UUID,
    val content: String,
    val createdAt: OffsetDateTime,
    val type: Int,
    val medias: List<ChatMessageMediaVm>,
)

data class ChatMessageMediaVm(
    val fileName: String,
    val contentType: String,
    val data: String,
)
