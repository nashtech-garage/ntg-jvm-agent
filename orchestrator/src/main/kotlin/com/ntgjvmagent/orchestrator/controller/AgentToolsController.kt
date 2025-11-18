package com.ntgjvmagent.orchestrator.controller

import com.ntgjvmagent.orchestrator.service.AgentToolAssignmentService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/agents/{agentId}/tools")
@PreAuthorize("hasRole('ROLE_ADMIN')")
class AgentToolsController(
    private val toolAssignmentService: AgentToolAssignmentService,
) {
    @GetMapping
    fun list(
        @PathVariable agentId: UUID,
    ) = toolAssignmentService.getTools(agentId)

    @PostMapping("/{toolId}")
    fun assign(
        @PathVariable agentId: UUID,
        @PathVariable toolId: UUID,
    ) = toolAssignmentService.assignTool(agentId, toolId)

    @DeleteMapping("/{toolId}")
    fun unassign(
        @PathVariable agentId: UUID,
        @PathVariable toolId: UUID,
    ) = toolAssignmentService.unassignTool(agentId, toolId)
}
