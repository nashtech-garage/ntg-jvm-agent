package com.ntgjvmagent.orchestrator.dto.response

data class AgentKnowledgeImportResponseDto(
    val knowledges: List<AgentKnowledgeResponseDto> = emptyList(),
    val fileNames: List<String> = emptyList(),
)
