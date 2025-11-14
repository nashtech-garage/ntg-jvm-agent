package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.entity.agent.mapping.AgentKnowledgeMapping
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeMappingRepository
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AgentKnowledgeAssignmentService(
    private val agentRepository: AgentRepository,
    private val knowledgeRepository: AgentKnowledgeRepository,
    private val mappingRepository: AgentKnowledgeMappingRepository,
) {
    @Transactional
    fun assignKnowledge(
        agentId: UUID,
        knowledgeId: UUID,
    ) {
        val agent = agentRepository.findByIdOrNull(agentId)
        val knowledge = knowledgeRepository.findByIdOrNull(knowledgeId)

        // Combine existence checks into one throw
        if (agent == null || knowledge == null) {
            throw EntityNotFoundException(
                buildString {
                    if (agent == null) append("Agent not found: $agentId. ")
                    if (knowledge == null) append("Knowledge not found: $knowledgeId.")
                }.trim(),
            )
        }

        // Use require for business rules
        require(knowledge.active) { "Knowledge is inactive and cannot be assigned" }
        require(!mappingRepository.existsByAgentIdAndKnowledgeId(agentId, knowledgeId)) {
            "Knowledge already assigned to this agent"
        }

        mappingRepository.save(AgentKnowledgeMapping.of(agent, knowledge))
    }

    @Transactional
    fun unassignKnowledge(
        agentId: UUID,
        knowledgeId: UUID,
    ) {
        mappingRepository.deleteByAgentIdAndKnowledgeId(agentId, knowledgeId)
    }

    @Transactional(readOnly = true)
    fun getKnowledge(agentId: UUID): List<AgentKnowledge> =
        mappingRepository.findByAgentId(agentId).map { it.knowledge }
}
