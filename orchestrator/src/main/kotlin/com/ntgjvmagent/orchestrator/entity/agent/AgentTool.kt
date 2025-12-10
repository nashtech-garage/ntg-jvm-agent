package com.ntgjvmagent.orchestrator.entity.agent

import com.ntgjvmagent.orchestrator.entity.Tool
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
@Table(name = "agent_tool")
class AgentTool(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    var agent: Agent,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tool_id", nullable = false)
    var tool: Tool,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var config: Map<String, Any>? = null,
    @Column(name = "active", nullable = false)
    var active: Boolean = true,
) : UserAuditedEntity() {
    companion object {
        fun of(
            agent: Agent,
            tool: Tool,
        ) = AgentTool(agent = agent, tool = tool)
    }
}
