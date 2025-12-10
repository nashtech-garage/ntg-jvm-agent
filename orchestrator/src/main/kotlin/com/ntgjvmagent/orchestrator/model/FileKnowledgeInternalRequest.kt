package com.ntgjvmagent.orchestrator.model

data class FileKnowledgeInternalRequest(
    val name: String,
    val metadata: Map<String, Any?> = emptyMap(),
)
