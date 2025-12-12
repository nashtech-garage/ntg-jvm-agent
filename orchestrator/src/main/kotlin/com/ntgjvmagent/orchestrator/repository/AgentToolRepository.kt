package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.dto.response.ToolWithAssignmentResponseDto
import com.ntgjvmagent.orchestrator.entity.agent.AgentTool
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AgentToolRepository : JpaRepository<AgentTool, UUID> {
    fun findByAgentId(agentId: UUID): List<AgentTool>

    fun existsByAgentIdAndToolId(
        agentId: UUID,
        toolId: UUID,
    ): Boolean

    fun findByAgentIdAndToolId(
        agentId: UUID,
        toolId: UUID,
    ): AgentTool?

    @Query(
        """
        SELECT
            t.id AS toolId,
            t.name AS toolName,
            t.description AS toolDescription,
            CASE WHEN at.agent_id IS NOT NULL THEN true ELSE false END AS isAssigned
        FROM tool t
        LEFT JOIN agent_tool at
            ON t.id = at.tool_id
            AND at.agent_id = :agentId
            AND at.active = true
        WHERE t.active = true
        ORDER BY t.name
        """,
        nativeQuery = true,
    )
    fun findToolsWithAssignment(
        @Param("agentId") agentId: UUID,
    ): List<ToolWithAssignmentResponseDto>

    @Query(
        """
        SELECT
            t.id AS toolId,
            t.name AS toolName,
            t.description AS toolDescription,
            CASE WHEN at.agent_id IS NOT NULL THEN true ELSE false END AS isAssigned
        FROM tool t
        LEFT JOIN agent_tool at
            ON t.id = at.tool_id
            AND at.agent_id = :agentId
            AND at.active = true
        WHERE t.active = true
            AND LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))
        ORDER BY t.name
        """,
        nativeQuery = true,
    )
    fun findToolsWithAssignmentAndSearch(
        @Param("agentId") agentId: UUID,
        @Param("name") name: String,
    ): List<ToolWithAssignmentResponseDto>

    @Modifying
    fun deleteByAgentIdAndToolId(
        agentId: UUID,
        toolId: UUID,
    )
}
