package com.ntgjvmagent.orchestrator.service

import com.ntgjvmagent.orchestrator.dto.SystemSettingRequestDto
import com.ntgjvmagent.orchestrator.dto.SystemSettingResponseDto
import com.ntgjvmagent.orchestrator.entity.SystemSettingEntity
import com.ntgjvmagent.orchestrator.exception.ResourceNotFoundException
import com.ntgjvmagent.orchestrator.repository.SystemSettingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SystemSettingService(
    private val systemSetting: SystemSettingRepository,
) {
    @Transactional(readOnly = true)
    fun getSystemSetting(): SystemSettingResponseDto =
        systemSetting
            .findAll()
            .firstOrNull()
            ?.toSystemSettingResponseDto()
            ?: throw ResourceNotFoundException("No system setting found")

    @Transactional
    fun updateSystemSetting(
        request: SystemSettingRequestDto,
    ): SystemSettingResponseDto {
        val setting =
            systemSetting
                .findAll()
                .firstOrNull()
                ?: throw ResourceNotFoundException("No system setting found")

        setting.siteName = request.siteName
        setting.maximumUser = request.maximumUser
        setting.sessionTimeout = request.sessionTimeout
        setting.maximumSizeFileUpload = request.maximumSizeFileUpload
        setting.allowedFileTypes = request.allowedFileTypes
        setting.maintenanceMode = request.maintenanceMode
        setting.userRegistration = request.userRegistration
        setting.emailVerification = request.emailVerification
        return systemSetting.save(setting).toSystemSettingResponseDto()
    }

    private fun SystemSettingEntity.toSystemSettingResponseDto() =
        SystemSettingResponseDto(
            id = this.id,
            siteName = this.siteName,
            maximumUser = this.maximumUser,
            sessionTimeout = this.sessionTimeout,
            maximumSizeFileUpload = this.maximumSizeFileUpload,
            allowedFileTypes = this.allowedFileTypes,
            maintenanceMode = this.maintenanceMode,
            userRegistration = this.userRegistration,
            emailVerification = this.emailVerification,
        )
}
