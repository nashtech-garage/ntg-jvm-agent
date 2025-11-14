package com.ntgjvmagent.orchestrator.dto

import com.ntgjvmagent.orchestrator.entity.agent.knowledge.KnowledgeChunk
import org.springframework.ai.document.Document
import java.util.UUID

data class KnowledgeChunkResponseDto(
    val id: UUID,
    val content: String,
    val metadata: Map<String, Any>?,
) {
    companion object {
        fun from(entity: KnowledgeChunk) =
            KnowledgeChunkResponseDto(
                id = entity.id!!,
                content = entity.content,
                metadata = entity.metadata,
            )

        fun fromDocument(doc: Document) =
            KnowledgeChunkResponseDto(
                id = UUID.fromString(doc.id),
                content = doc.text!!,
                metadata = doc.metadata,
            )
    }
}
