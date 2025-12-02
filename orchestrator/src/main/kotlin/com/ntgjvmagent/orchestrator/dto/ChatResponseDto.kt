package com.ntgjvmagent.orchestrator.dto

data class ChatResponseDto(
    val answer: String?,
    val citations: List<CitationDto>,
)
