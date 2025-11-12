package com.ntgjvmagent.orchestrator.controller

import com.ntgjvmagent.orchestrator.dto.AgentKnowledgeResponseDto
import com.ntgjvmagent.orchestrator.dto.AgentToolResponseDto
import com.ntgjvmagent.orchestrator.mapper.AgentKnowledgeMapper
import com.ntgjvmagent.orchestrator.mapper.AgentToolMapper
import com.ntgjvmagent.orchestrator.service.AgentKnowledgeAssignmentService
import com.ntgjvmagent.orchestrator.service.AgentToolAssignmentService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/agents/{agentId}")
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Tag(name = "Agent Assignments", description = "Assign tools and knowledge to agents")
class AgentAssignmentController(
    private val toolAssignmentService: AgentToolAssignmentService,
    private val knowledgeAssignmentService: AgentKnowledgeAssignmentService,
) {
    @PostMapping("/tools/{toolId}")
    fun assignTool(
        @PathVariable agentId: UUID,
        @PathVariable toolId: UUID,
    ) = toolAssignmentService.assignTool(agentId, toolId)

    @DeleteMapping("/tools/{toolId}")
    fun unassignTool(
        @PathVariable agentId: UUID,
        @PathVariable toolId: UUID,
    ) = toolAssignmentService.unassignTool(agentId, toolId)

    @GetMapping("/tools")
    fun listTools(
        @PathVariable agentId: UUID,
    ): List<AgentToolResponseDto> = toolAssignmentService.getTools(agentId).map(AgentToolMapper::toResponse)

    @PostMapping("/knowledge/{knowledgeId}")
    fun assignKnowledge(
        @PathVariable agentId: UUID,
        @PathVariable knowledgeId: UUID,
    ) = knowledgeAssignmentService.assignKnowledge(agentId, knowledgeId)

    @DeleteMapping("/knowledge/{knowledgeId}")
    fun unassignKnowledge(
        @PathVariable agentId: UUID,
        @PathVariable knowledgeId: UUID,
    ) = knowledgeAssignmentService.unassignKnowledge(agentId, knowledgeId)

    @GetMapping("/knowledge")
    fun listKnowledge(
        @PathVariable agentId: UUID,
    ): List<AgentKnowledgeResponseDto> =
        knowledgeAssignmentService.getKnowledge(agentId).map(AgentKnowledgeMapper::toResponse)
}
