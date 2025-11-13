package com.ntgjvmagent.orchestrator.controller

import com.ntgjvmagent.orchestrator.dto.AgentToolRequestDto
import com.ntgjvmagent.orchestrator.dto.AgentToolResponseDto
import com.ntgjvmagent.orchestrator.service.AgentToolService
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
@RequestMapping("/api/tools")
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Tag(name = "Agent Tools", description = "Manage AI Tools assignable to agents")
class AgentToolController(
    private val service: AgentToolService,
) {
    @GetMapping
    fun getAllActive(): List<AgentToolResponseDto> = service.getAllActive()

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: UUID,
    ): AgentToolResponseDto = service.getById(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody req: AgentToolRequestDto,
    ): AgentToolResponseDto = service.create(req)

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody req: AgentToolRequestDto,
    ): AgentToolResponseDto = service.update(id, req)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: UUID,
    ) = service.softDelete(id)
}
