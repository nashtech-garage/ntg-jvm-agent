package com.ntgjvmagent.orchestrator.integration.agent

import com.ntgjvmagent.orchestrator.entity.agent.Agent
import com.ntgjvmagent.orchestrator.integration.BaseIntegrationTest
import com.ntgjvmagent.orchestrator.repository.AgentRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Transactional
@DisplayName("AgentRepository Integration Tests")
class AgentRepositoryIT
    @Autowired
    constructor(
        private val repo: AgentRepository,
    ) : BaseIntegrationTest() {
        @Test
        @DisplayName("should save and retrieve agent successfully")
        fun shouldSaveAndFindAgent() {
            val agent =
                Agent(
                    name = "Repo Agent",
                    model = "gpt-4o-mini",
                    description = "Repository test",
                    temperature = BigDecimal("0.8"),
                )

            val saved = repo.save(agent)
            val found = repo.findById(saved.id!!)

            assertThat(found).isPresent
            assertThat(found.get().name).isEqualTo("Repo Agent")
            assertThat(found.get().temperature).isEqualTo(BigDecimal("0.8"))
        }

        @Test
        @DisplayName("should find all agents")
        fun shouldFindAllAgents() {
            repo.save(Agent(name = "Agent 1", model = "gpt-4o-mini"))
            repo.save(Agent(name = "Agent 2", model = "gpt-4o-mini"))

            val all = repo.findAll()
            assertThat(all).hasSizeGreaterThanOrEqualTo(2)
        }
    }
