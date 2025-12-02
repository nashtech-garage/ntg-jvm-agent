package com.ntgjvmagent.orchestrator.dto

data class CitationDto(
    val chunkId: String,
    val fileName: String,
    val filePath: String,
    val charStart: Int,
    val charEnd: Int,
)
