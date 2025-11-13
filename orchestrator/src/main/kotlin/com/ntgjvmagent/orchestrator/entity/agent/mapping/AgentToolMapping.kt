package com.ntgjvmagent.orchestrator.entity.agent.mapping

import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.entity.agent.AgentTool
import com.ntgjvmagent.orchestrator.entity.base.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "agent_tool_mapping",
    uniqueConstraints = [UniqueConstraint(columnNames = ["agent_id", "tool_id"])],
)
class AgentToolMapping(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    var agent: Agent,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tool_id", nullable = false)
    var tool: AgentTool,
) : BaseEntity() {
    companion object {
        fun of(
            agent: Agent,
            tool: AgentTool,
        ) = AgentToolMapping(agent, tool)
    }
}
