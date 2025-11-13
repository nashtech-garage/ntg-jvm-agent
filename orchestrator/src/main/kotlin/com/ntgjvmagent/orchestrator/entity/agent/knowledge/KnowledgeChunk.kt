package com.ntgjvmagent.orchestrator.entity.agent.knowledge

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime
import java.util.UUID

@Suppress("LongParameterList")
@Entity
@Table(name = "knowledge_chunk")
class KnowledgeChunk(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    var id: UUID? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_id", nullable = false)
    var knowledge: AgentKnowledge,
    @Column(name = "chunk_order", nullable = false)
    val chunkOrder: Int = 0,
    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var metadata: Map<String, Any>? = null,
    @Column(name = "embedding")
    @JdbcTypeCode(SqlTypes.VECTOR)
    var embedding: FloatArray,
    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    var createdAt: OffsetDateTime? = null,
)
