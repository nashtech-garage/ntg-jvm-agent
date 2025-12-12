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
        SELECT t.base_url AS baseUrl, t.connection_config AS config
        FROM tool t
        WHERE t.base_url IS NOT NULL AND t.active = true
        GROUP BY t.base_url, t.connection_config
        """,
        nativeQuery = true,
    )
    fun findActiveExternalTools(): List<ExternalToolDto>
}
