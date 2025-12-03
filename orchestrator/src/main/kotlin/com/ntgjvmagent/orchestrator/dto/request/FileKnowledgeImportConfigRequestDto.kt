package com.ntgjvmagent.orchestrator.dto.request

import jakarta.validation.constraints.NotBlank

data class FileKnowledgeImportConfigRequestDto(
    @field:NotBlank
    val name: String,
    val metadata: Map<String, Any?> = emptyMap(),
)
