package com.ntgjvmagent.orchestrator.entity

import com.ntgjvmagent.orchestrator.entity.agent.AgentTool
import com.ntgjvmagent.orchestrator.entity.base.SoftDeletableEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "tool")
data class Tool(
    @Column(nullable = false, length = 100)
    var name: String,
    @Column(length = 50)
    var type: String? = null, // e.g., "search", "retrieval", "custom"
    @Column(columnDefinition = "TEXT")
    var description: String? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var config: Map<String, Any>? = null,
) : SoftDeletableEntity() {
    @OneToMany(mappedBy = "tool", cascade = [CascadeType.ALL], orphanRemoval = true)
    var agents: MutableSet<AgentTool> = mutableSetOf()
}
