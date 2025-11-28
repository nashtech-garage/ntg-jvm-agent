package com.ntgjvmagent.orchestrator.controller

import com.ntgjvmagent.orchestrator.dto.AgentListResponseDto
import com.ntgjvmagent.orchestrator.dto.AgentRequestDto
import com.ntgjvmagent.orchestrator.dto.AgentResponseDto
import com.ntgjvmagent.orchestrator.service.AgentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/agents")
@Tag(name = "Agent Management", description = "Manage AI Agents for orchestration")
class AgentController(
    private val service: AgentService,
) {
    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "List all agents")
    fun getAll(): List<AgentListResponseDto> = service.getAll()

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('SCOPE_agents.read') or hasRole('ROLE_ADMIN') or hasRole('ROLE_CLIENT')")
    @Operation(summary = "List all active agents")
    fun getAllActive(): List<AgentResponseDto> = service.getAllActive()

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_agents.read') or hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get agent by ID")
    fun getById(
        @PathVariable id: UUID,
    ): AgentResponseDto = service.getById(id)

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_agents.write') or hasRole('ROLE_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new agent")
    fun create(
        @Valid @RequestBody request: AgentRequestDto,
    ): AgentResponseDto = service.create(request)

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_agents.write') or hasRole('ROLE_ADMIN')")
    @Operation(summary = "Update an existing agent")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: AgentRequestDto,
    ): AgentResponseDto = service.update(id, request)

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_agents.write') or hasRole('ROLE_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft delete an agent")
    fun delete(
        @PathVariable id: UUID,
    ) = service.softDelete(id)
}
