package com.ntgjvmagent.orchestrator.dto.response

import java.util.UUID

data class SystemSettingResponseDto(
    val id: UUID?,
    val siteName: String?,
    val maximumUser: Int?,
    val sessionTimeout: Int?,
    val maximumSizeFileUpload: Int?,
    val allowedFileTypes: String?,
    val maintenanceMode: Boolean?,
    val userRegistration: Boolean?,
    val emailVerification: Boolean?,
)
