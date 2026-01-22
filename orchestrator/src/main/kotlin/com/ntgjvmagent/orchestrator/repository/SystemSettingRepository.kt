package com.ntgjvmagent.orchestrator.repository

import com.ntgjvmagent.orchestrator.entity.SystemSetting
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SystemSettingRepository : JpaRepository<SystemSetting, UUID>
