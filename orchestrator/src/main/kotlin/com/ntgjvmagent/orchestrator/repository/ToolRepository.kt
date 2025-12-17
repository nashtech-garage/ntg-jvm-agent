package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.dto.ExternalToolDto
import com.ntgjvmagent.orchestrator.entity.Tool
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ToolRepository : JpaRepository<Tool, UUID> {
    fun findAllByActiveTrue(): List<Tool>

    @Query(
        """
        SELECT DISTINCT t.baseUrl AS baseUrl, t.connectionConfig AS config
        FROM Tool t
        WHERE t.baseUrl IS NOT NULL AND t.active = true
        """,
    )
    fun findActiveExternalTools(): List<ExternalToolDto>
}
