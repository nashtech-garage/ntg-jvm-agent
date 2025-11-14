package com.ntgjvmagent.orchestrator.entity.agent.mapping

import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.entity.base.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "agent_knowledge_mapping",
    uniqueConstraints = [UniqueConstraint(columnNames = ["agent_id", "knowledge_id"])],
)
class AgentKnowledgeMapping(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    var agent: Agent,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_id", nullable = false)
    var knowledge: AgentKnowledge,
) : BaseEntity() {
    companion object {
        fun of(
            agent: Agent,
            knowledge: AgentKnowledge,
        ) = AgentKnowledgeMapping(agent, knowledge)
    }
}
