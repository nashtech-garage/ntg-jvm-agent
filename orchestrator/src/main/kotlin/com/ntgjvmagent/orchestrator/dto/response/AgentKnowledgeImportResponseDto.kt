package com.ntgjvmagent.orchestrator.dto.response

data class AgentKnowledgeImportResponseDto(
    val knowledge: List<AgentKnowledgeResponseDto> = emptyList(),
    val fileNames: List<String> = emptyList(),
)
