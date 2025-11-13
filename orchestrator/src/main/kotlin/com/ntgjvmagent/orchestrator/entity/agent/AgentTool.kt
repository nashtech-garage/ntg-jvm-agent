package com.ntgjvmagent.orchestrator.entity.agent

import com.ntgjvmagent.orchestrator.entity.base.SoftDeletableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "agent_tool")
data class AgentTool(
    @Column(nullable = false, length = 100)
    var name: String,
    @Column(length = 50)
    var type: String? = null, // e.g., "search", "retrieval", "custom"
    @Column(columnDefinition = "TEXT")
    var description: String? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var config: Map<String, Any>? = null,
) : SoftDeletableEntity()
