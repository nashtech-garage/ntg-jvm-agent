package com.ntgjvmagent.orchestrator.controller

import com.ntgjvmagent.orchestrator.dto.ToolRequestDto
import com.ntgjvmagent.orchestrator.dto.ToolResponseDto
import com.ntgjvmagent.orchestrator.service.ToolService
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
@Tag(name = "Tools", description = "Manage AI Tools assignable to agents")
class ToolController(
    private val service: ToolService,
) {
    @GetMapping
    fun getAllActive(): List<ToolResponseDto> = service.getAllActive()

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: UUID,
    ): ToolResponseDto = service.getById(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody req: ToolRequestDto,
    ): ToolResponseDto = service.create(req)

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody req: ToolRequestDto,
    ): ToolResponseDto = service.update(id, req)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: UUID,
    ) = service.softDelete(id)
}
