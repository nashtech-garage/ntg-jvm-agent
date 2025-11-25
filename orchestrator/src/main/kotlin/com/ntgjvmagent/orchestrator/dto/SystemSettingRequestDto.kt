package com.ntgjvmagent.orchestrator.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class SystemSettingRequestDto(
    @field:NotBlank val siteName: String,
    @field:NotNull val maximumUser: Int,
    @field:NotNull val sessionTimeout: Int,
    @field:NotNull val maximumSizeFileUpload: Int,
    @field:NotBlank val allowedFileTypes: String,
    @field:NotNull val maintenanceMode: Boolean,
    @field:NotNull val userRegistration: Boolean,
    @field:NotNull val emailVerification: Boolean,
)
