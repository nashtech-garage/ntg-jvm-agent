package com.ntgjvmagent.orchestrator.dto

data class AgentKnowledgeImportResponseDto(
    val knowledge: AgentKnowledgeResponseDto,
    val numberOfChunks: Int,
    val fileName: String,
)
