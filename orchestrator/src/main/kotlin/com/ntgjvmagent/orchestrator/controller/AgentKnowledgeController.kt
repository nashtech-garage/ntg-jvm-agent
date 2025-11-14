package com.ntgjvmagent.orchestrator.controller

import com.ntgjvmagent.orchestrator.dto.AgentKnowledgeRequestDto
import com.ntgjvmagent.orchestrator.dto.AgentKnowledgeResponseDto
import com.ntgjvmagent.orchestrator.service.AgentKnowledgeService
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
@RequestMapping("/api/knowledge")
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Tag(name = "Agent Knowledge", description = "Manage knowledge sources assignable to agents")
class AgentKnowledgeController(
    private val service: AgentKnowledgeService,
) {
    @GetMapping
    fun getAllActive(): List<AgentKnowledgeResponseDto> = service.getAllActive()

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: UUID,
    ): AgentKnowledgeResponseDto = service.getById(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody req: AgentKnowledgeRequestDto,
    ): AgentKnowledgeResponseDto = service.create(req)

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody req: AgentKnowledgeRequestDto,
    ): AgentKnowledgeResponseDto = service.update(id, req)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: UUID,
    ) = service.softDelete(id)
}
