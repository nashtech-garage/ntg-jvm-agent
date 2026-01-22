package com.ntgjvmagent.orchestrator.entity

import com.ntgjvmagent.orchestrator.entity.base.UserAuditedEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "system_setting")
data class SystemSetting(
    @Column(name = "site_name")
    var siteName: String,
    @Column(name = "maximum_user")
    var maximumUser: Int,
    @Column(name = "session_timeout")
    var sessionTimeout: Int,
    @Column(name = "maximum_size_file_upload")
    var maximumSizeFileUpload: Int,
    @Column(name = "allowed_file_types")
    var allowedFileTypes: String,
    @Column(name = "maintenance_mode")
    var maintenanceMode: Boolean,
    @Column(name = "user_registration")
    var userRegistration: Boolean,
    @Column(name = "email_verification")
    var emailVerification: Boolean,
) : UserAuditedEntity()
