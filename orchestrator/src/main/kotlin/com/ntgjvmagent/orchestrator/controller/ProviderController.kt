package com.ntgjvmagent.orchestrator.controller

import com.ntgjvmagent.orchestrator.dto.response.ProviderNameResponseDto
import com.ntgjvmagent.orchestrator.dto.response.ProviderResponseDto
import com.ntgjvmagent.orchestrator.service.ProviderService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/providers")
class ProviderController(
    private val providerService: ProviderService,
) {
    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    fun getActiveProviders(): List<ProviderNameResponseDto> = providerService.getActiveProviderNames()

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_agents.read') or hasRole('ROLE_ADMIN')")
    fun getById(
        @PathVariable id: UUID,
    ): ResponseEntity<ProviderResponseDto> {
        val providerResponseDto = providerService.getById(id)
        return ResponseEntity.ok(providerResponseDto)
    }
}
