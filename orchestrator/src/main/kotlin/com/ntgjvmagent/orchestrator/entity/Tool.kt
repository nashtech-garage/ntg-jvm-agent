package com.ntgjvmagent.orchestrator.entity

import com.ntgjvmagent.orchestrator.entity.agent.AgentTool
import com.ntgjvmagent.orchestrator.entity.base.SoftDeletableEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "tool")
@Audited
data class Tool(
    @Column(nullable = false, length = 100)
    var name: String,
    @Column(length = 50)
    var type: String? = null, // e.g., "search", "retrieval", "custom"
    @Column(columnDefinition = "TEXT")
    var description: String? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var definition: Map<String, Any>? = null,
    // e.g. { endpoint, type ("stdio", "sse", "streamable"), authorization }
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "connection_config", columnDefinition = "jsonb")
    var connectionConfig: Map<String, Any>? = null,
    @Column(name = "base_url")
    @NotAudited
    var baseUrl: String? = null,
) : SoftDeletableEntity() {
    @OneToMany(mappedBy = "tool", cascade = [CascadeType.ALL], orphanRemoval = true)
    @NotAudited
    var agents: MutableSet<AgentTool> = mutableSetOf()
}
