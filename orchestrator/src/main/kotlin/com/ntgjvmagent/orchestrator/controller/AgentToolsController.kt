package com.ntgjvmagent.orchestrator.controller

import com.ntgjvmagent.orchestrator.dto.AgentToolStatusDto
import com.ntgjvmagent.orchestrator.service.AgentToolService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/agents/{agentId}/tools")
@PreAuthorize("hasRole('ROLE_ADMIN')")
class AgentToolsController(
    private val agentToolService: AgentToolService,
) {
    @GetMapping
    fun list(
        @PathVariable agentId: UUID,
    ) = agentToolService.getTools(agentId)

    @GetMapping("/assignment")
    fun listToolsWithAssignmentStatus(
        @PathVariable agentId: UUID,
        @RequestParam(required = false) name: String?,
    ) = agentToolService.getToolsWithAssignmentStatus(agentId, name)

    @PostMapping("/{toolId}")
    fun assign(
        @PathVariable agentId: UUID,
        @PathVariable toolId: UUID,
    ) = agentToolService.assignTool(agentId, toolId)

    @PutMapping("/{toolId}")
    fun updateStatus(
        @PathVariable agentId: UUID,
        @PathVariable toolId: UUID,
        @RequestBody agentToolStatus: AgentToolStatusDto,
    ) = agentToolService.updateStatus(agentId, toolId, agentToolStatus.status)

    @DeleteMapping("/{toolId}")
    fun unassign(
        @PathVariable agentId: UUID,
        @PathVariable toolId: UUID,
    ) = agentToolService.unassignTool(agentId, toolId)
}
