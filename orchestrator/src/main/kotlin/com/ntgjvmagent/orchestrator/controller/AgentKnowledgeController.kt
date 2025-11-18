package com.ntgjvmagent.orchestrator.controller

import com.ntgjvmagent.orchestrator.dto.AgentKnowledgeRequestDto
import com.ntgjvmagent.orchestrator.dto.AgentKnowledgeResponseDto
import com.ntgjvmagent.orchestrator.service.AgentKnowledgeService
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
@RequestMapping("/api/agents/{agentId}/knowledge")
@PreAuthorize("hasRole('ROLE_ADMIN')")
class AgentKnowledgeController(
    private val agentKnowledgeService: AgentKnowledgeService,
) {
    @GetMapping
    fun list(
        @PathVariable agentId: UUID,
    ) = agentKnowledgeService.getByAgent(agentId)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable agentId: UUID,
        @Valid @RequestBody request: AgentKnowledgeRequestDto,
    ): AgentKnowledgeResponseDto = agentKnowledgeService.create(agentId, request)

    @PutMapping("/{knowledgeId}")
    fun update(
        @PathVariable agentId: UUID,
        @PathVariable knowledgeId: UUID,
        @Valid @RequestBody request: AgentKnowledgeRequestDto,
    ): AgentKnowledgeResponseDto = agentKnowledgeService.update(agentId, knowledgeId, request)

    @GetMapping("/{knowledgeId}")
    fun get(
        @PathVariable agentId: UUID,
        @PathVariable knowledgeId: UUID,
    ): AgentKnowledgeResponseDto = agentKnowledgeService.getOneForAgent(agentId, knowledgeId)

    @DeleteMapping("/{knowledgeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable agentId: UUID,
        @PathVariable knowledgeId: UUID,
    ) = agentKnowledgeService.softDelete(agentId, knowledgeId)
}
