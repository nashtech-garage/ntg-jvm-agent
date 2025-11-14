package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.agent.AgentTool
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AgentToolRepository : JpaRepository<AgentTool, UUID> {
    fun findAllByActiveTrue(): List<AgentTool>
}
