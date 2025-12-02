package com.ntgjvmagent.orchestrator.viewmodel

import java.util.UUID

data class KnowledgeImportingResponseVm(
    val originalFilename: String,
    val numberOfSegment: Int,
    val fileId: UUID,
    val filePath: String,
)
