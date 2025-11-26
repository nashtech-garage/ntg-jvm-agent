package com.ntgjvmagent.orchestrator.entity.agent.knowledge

import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.entity.base.SoftDeletableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "agent_knowledge")
data class AgentKnowledge(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    var agent: Agent,
    @Column(nullable = false, length = 100)
    var name: String,
    @Column(name = "source_type", length = 50)
    var sourceType: String? = null, // e.g., "url", "pdf", "repo", "dataset"
    @Column(name = "source_uri")
    var sourceUri: String? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var metadata: Map<String, Any>? = null,
) : SoftDeletableEntity()
