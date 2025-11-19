package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.Tool
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ToolRepository : JpaRepository<Tool, UUID> {
    fun findAllByActiveTrue(): List<Tool>
}
