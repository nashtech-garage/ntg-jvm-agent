package com.ntgjvmagent.orchestrator.controller

import com.ntgjvmagent.orchestrator.dto.SystemSettingRequestDto
import com.ntgjvmagent.orchestrator.dto.SystemSettingResponseDto
import com.ntgjvmagent.orchestrator.service.SystemSettingService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/settings")
@PreAuthorize("hasRole('ROLE_ADMIN')")
class SystemSettingController(
    private val systemSettingService: SystemSettingService,
) {
    @GetMapping
    fun getSystemSetting(): ResponseEntity<SystemSettingResponseDto> =
        ResponseEntity.ok(systemSettingService.getSystemSetting())

    @PutMapping("/{id}")
    fun updateSystemSetting(
        @PathVariable id: UUID,
        @Valid @RequestBody request: SystemSettingRequestDto,
    ): ResponseEntity<SystemSettingResponseDto> =
        ResponseEntity.ok(systemSettingService.updateSystemSetting(id, request))
}
