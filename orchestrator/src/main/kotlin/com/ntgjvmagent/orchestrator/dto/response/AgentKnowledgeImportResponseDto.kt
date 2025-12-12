package com.ntgjvmagent.orchestrator.dto.response

data class AgentKnowledgeImportResponseDto(
    val knowledge: AgentKnowledgeResponseDto,
    val fileNames: List<String> = emptyList(),
)
