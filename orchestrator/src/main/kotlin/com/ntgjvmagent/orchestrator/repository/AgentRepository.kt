package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.agent.Agent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AgentRepository : JpaRepository<Agent, UUID> {
    fun findAllByActiveTrue(): List<Agent>
}
