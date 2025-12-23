package com.ntgjvmagent.orchestrator.entity.agent.knowledge

import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.entity.base.BaseEntity
import com.ntgjvmagent.orchestrator.ingestion.job.IngestionJobStatus
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
@Table(name = "ingestion_job")
class IngestionJob(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    val agent: Agent,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_knowledge_id", nullable = false)
    val knowledge: AgentKnowledge,
) : BaseEntity() {
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: IngestionJobStatus = IngestionJobStatus.PENDING

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

    companion object {
        const val DEFAULT_MAX_ATTEMPTS = 3
    }

    // ---- Domain Methods ----
    fun markRunning() {
        status = IngestionJobStatus.RUNNING
        startedAt = Instant.now()
    }

    fun markSuccess() {
        status = IngestionJobStatus.SUCCESS
        finishedAt = Instant.now()
    }

    fun markFailed(error: String?) {
        status = IngestionJobStatus.FAILED
        errorMessage = error
        finishedAt = Instant.now()
    }

    fun incrementAttempts() {
        attempts++
    }
}
