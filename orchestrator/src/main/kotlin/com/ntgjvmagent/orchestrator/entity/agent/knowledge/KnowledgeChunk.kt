package com.ntgjvmagent.orchestrator.entity.agent.knowledge

import com.ntgjvmagent.orchestrator.entity.base.UserAuditedEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "knowledge_chunk")
class KnowledgeChunk(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_id", nullable = false)
    var knowledge: AgentKnowledge,
    @Column(name = "chunk_order", nullable = false)
    val chunkOrder: Int = 0,
    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var metadata: Map<String, Any?> = emptyMap(),
) : UserAuditedEntity() {
    companion object {
        fun stub(): KnowledgeChunk =
            KnowledgeChunk(
                knowledge = AgentKnowledge.stub(),
                chunkOrder = 0,
                content = "",
            )
    }
}
