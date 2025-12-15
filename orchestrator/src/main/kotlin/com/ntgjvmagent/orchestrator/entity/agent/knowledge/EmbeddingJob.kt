package com.ntgjvmagent.orchestrator.entity.agent.knowledge

import com.ntgjvmagent.orchestrator.embedding.EmbeddingJobStatus
import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.entity.base.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "embedding_job")
class EmbeddingJob(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    val agent: Agent,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_knowledge_id", nullable = false)
    val knowledge: AgentKnowledge,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chunk_id", nullable = false)
    val chunk: KnowledgeChunk,
) : BaseEntity() {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: EmbeddingJobStatus = EmbeddingJobStatus.PENDING

    @Column(nullable = false)
    var attempts: Int = 0

    @Column(name = "max_attempts", nullable = false)
    var maxAttempts: Int = DEFAULT_MAX_ATTEMPTS

    @Column(name = "error_message")
    var errorMessage: String? = null

    @Column(name = "started_at")
    var startedAt: Instant? = null

    @Column(name = "finished_at")
    var finishedAt: Instant? = null

    @Column(name = "next_attempt_at")
    var nextAttemptAt: Instant? = null

    companion object {
        const val DEFAULT_MAX_ATTEMPTS = 3
    }
}
