package com.ntgjvmagent.orchestrator.entity.agent.knowledge

import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.entity.base.SoftDeletableEntity
import com.ntgjvmagent.orchestrator.model.KnowledgeSourceType
import com.ntgjvmagent.orchestrator.model.KnowledgeStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "agent_knowledge")
data class AgentKnowledge(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    var agent: Agent,
    @Column(nullable = false, updatable = false, length = 100)
    var name: String,
    @Column(name = "original_file_name")
    var originalFileName: String? = null,
    @Column(name = "checksum_sha256")
    var checksumSha256: String? = null,
    @Column(name = "file_size_bytes")
    var fileSizeBytes: Long? = null,
    @Column(name = "storage_key")
    var storageKey: String? = null,
    @Column(name = "source_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var sourceType: KnowledgeSourceType,
    @Column(name = "source_uri")
    var sourceUri: String? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var metadata: Map<String, Any?> = emptyMap(),
    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    var status: KnowledgeStatus = KnowledgeStatus.PENDING,
    @Column(name = "last_processed_at")
    var lastProcessedAt: Instant? = null,
    @Column(name = "error_message")
    var errorMessage: String? = null,
) : SoftDeletableEntity() {
    companion object {
        fun stub(): AgentKnowledge =
            AgentKnowledge(
                agent = Agent.stub(),
                name = "",
                sourceType = KnowledgeSourceType.INLINE,
            )
    }

    fun markIngesting() {
        status = KnowledgeStatus.INGESTING
        errorMessage = null
    }

    fun markEmbeddingPending() {
        status = KnowledgeStatus.EMBEDDING_PENDING
        lastProcessedAt = Instant.now()
        errorMessage = null
    }

    fun markFailed(message: String?) {
        status = KnowledgeStatus.FAILED
        errorMessage = message
    }
}
