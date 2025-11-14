package com.ntgjvmagent.orchestrator.integration.agent

import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.entity.agent.knowledge.AgentKnowledge
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.repository.AgentKnowledgeRepository
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import com.ntgjvmagent.orchestrator.repository.KnowledgeChunkRepository
import com.ntgjvmagent.orchestrator.service.AgentKnowledgeAssignmentService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.Test

class AgentKnowledgeAssignmentServiceIT
    @Autowired
    constructor(
        private val service: AgentKnowledgeAssignmentService,
        private val agentRepo: AgentRepository,
        private var knowledgeRepo: AgentKnowledgeRepository,
        private var chunkRepository: KnowledgeChunkRepository,
    ) : BaseIntegrationTest() {
        @BeforeEach
        fun cleanup() {
            chunkRepository.deleteAllInBatch()
            knowledgeRepo.deleteAllInBatch()
        }

        @Test
        fun `assign and unassign knowledge`() {
            val agent = agentRepo.save(Agent(name = "A1-${UUID.randomUUID()}", model = "M1"))
            val knowledge = knowledgeRepo.save(AgentKnowledge(name = "K1-${UUID.randomUUID()}"))

            service.assignKnowledge(agent.id!!, knowledge.id!!)
            val list = service.getKnowledge(agent.id!!)
            assertEquals(1, list.size)

            service.unassignKnowledge(agent.id!!, knowledge.id!!)
            val listAfter = service.getKnowledge(agent.id!!)
            assertTrue(listAfter.isEmpty())
        }
    }
